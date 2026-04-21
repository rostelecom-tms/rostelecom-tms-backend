package ru.rt.rostelecom_tms.service.search;

import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.CaseStep;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseNotFoundException;
import ru.rt.rostelecom_tms.repository.cases.CaseRepository;
import ru.rt.rostelecom_tms.service.embedding.EmbeddingClient;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseEmbeddingService {

    private static final int MAX_TOP_K = 10;
    private static final double SIMILARITY_THRESHOLD = 0.82;

    private final EmbeddingClient embeddingClient;
    private final JdbcTemplate jdbcTemplate;
    private final CaseRepository caseRepository;

    @Transactional
    public void index(int caseId) {
        index(caseId, null);
    }

    @Transactional
    public void index(int caseId, String providerOverride) {
        Case testCase = caseRepository.findOneById(caseId)
                .orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + caseId));

        float[] vector = toFloatArray(embeddingClient.embed(buildCaseText(testCase), providerOverride));

        jdbcTemplate.update(
                """
                INSERT INTO case_embeddings (case_id, embedding, updated_at)
                VALUES (?, ?::vector, NOW())
                ON CONFLICT (case_id)
                DO UPDATE SET embedding = EXCLUDED.embedding, updated_at = NOW()
                """,
                testCase.getId(),
                new PGvector(vector)
        );
    }

    @Transactional
    public void indexAll(String providerOverride) {
        caseRepository.findAll().forEach(testCase -> index(testCase.getId(), providerOverride));
    }

    @Async("embeddingTaskExecutor")
    public CompletableFuture<Void> indexAllAsync(String providerOverride) {
        try {
            log.info("Started case embeddings reindex-all (providerOverride={})", providerOverride);
            indexAll(providerOverride);
            log.info("Completed case embeddings reindex-all (providerOverride={})", providerOverride);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to reindex all case embeddings", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional
    public void delete(int caseId) {
        jdbcTemplate.update("DELETE FROM case_embeddings WHERE case_id = ?", caseId);
    }

    public List<SimilarCaseResult> findSimilar(int caseId, int limit) {
        return findSimilar(caseId, limit, null);
    }

    public List<SimilarCaseResult> findSimilar(int caseId, int limit, String providerOverride) {
        if (providerOverride != null) {
            log.warn(
                    "findSimilar with providerOverride={} for caseId={}. " +
                            "Stored vectors may have been indexed with a different provider. " +
                            "Results may be inaccurate if providers differ.",
                    providerOverride, caseId
            );
            Case testCase = caseRepository.findOneById(caseId)
                    .orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + caseId));
            float[] vector = toFloatArray(embeddingClient.embed(buildCaseText(testCase), providerOverride));
            return findSimilarByVector(vector, normalizeLimit(limit), caseId);
        }

        float[] storedVector = loadStoredVector(caseId);
        if (storedVector == null) {
            log.warn("Case {} has no stored embedding. Run reindex first.", caseId);
            return List.of();
        }
        return findSimilarByVector(storedVector, normalizeLimit(limit), caseId);
    }

    public List<SimilarCaseResult> findSimilarByText(String text, int limit) {
        return findSimilarByText(text, limit, null);
    }

    public List<SimilarCaseResult> findSimilarByText(String text, int limit, String providerOverride) {
        float[] vector = toFloatArray(embeddingClient.embed(text, providerOverride));
        return findSimilarByVector(vector, normalizeLimit(limit), null);
    }

    float[] loadStoredVector(int caseId) {
        List<float[]> rows = jdbcTemplate.query(
                "SELECT embedding::text FROM case_embeddings WHERE case_id = ?",
                (rs, rowNum) -> parseVector(rs.getString(1)),
                caseId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public String buildCaseText(Case testCase) {
        StringBuilder sb = new StringBuilder(testCase.getTitle());
        if (testCase.getDescription() != null) {
            sb.append("\n").append(testCase.getDescription());
        }
        if (testCase.getPreconditions() != null) {
            sb.append("\nPreconditions: ").append(testCase.getPreconditions());
        }
        if (testCase.getPostconditions() != null) {
            sb.append("\nPostconditions: ").append(testCase.getPostconditions());
        }
        testCase.getCaseSteps().stream()
                .sorted(Comparator.comparingInt(CaseStep::getOrder))
                .forEach(step -> {
                    sb.append("\n").append(step.getOrder()).append(". ").append(step.getAction());
                    if (step.getExpectedResult() != null) {
                        sb.append(" => ").append(step.getExpectedResult());
                    }
                });
        return sb.toString();
    }

    private List<SimilarCaseResult> findSimilarByVector(float[] vector, int limit, Integer excludeCaseId) {
        String sql = """
            SELECT ce.case_id,
                   (1 - (ce.embedding <=> ?::vector)) AS score
            FROM case_embeddings ce
            WHERE (? IS NULL OR ce.case_id <> ?)
              AND (1 - (ce.embedding <=> ?::vector)) >= ?
            ORDER BY ce.embedding <=> ?::vector
            LIMIT ?
            """;

        PGvector pgv = new PGvector(vector);
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SimilarCaseResult(rs.getInt("case_id"), rs.getDouble("score")),
                pgv, excludeCaseId, excludeCaseId, pgv, SIMILARITY_THRESHOLD, pgv, limit
        );
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, MAX_TOP_K));
    }

    private float[] parseVector(String pgVectorText) {
        if (pgVectorText == null || pgVectorText.isBlank()) {
            return new float[0];
        }

        String cleaned = pgVectorText.trim();
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.isBlank()) {
            return new float[0];
        }

        String[] parts = cleaned.split(",");
        float[] arr = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            arr[i] = Float.parseFloat(parts[i].trim());
        }
        return arr;
    }

    private float[] toFloatArray(List<Double> vector) {
        float[] arr = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            arr[i] = vector.get(i).floatValue();
        }
        return arr;
    }

    public record SimilarCaseResult(
            Integer caseId,
            Double score
    ) {}
}