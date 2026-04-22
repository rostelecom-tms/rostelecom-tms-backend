package ru.rt.rostelecom_tms.service.search;

import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.cases.Defect;
import ru.rt.rostelecom_tms.domain.cases.exceptions.DefectNotFoundException;
import ru.rt.rostelecom_tms.repository.cases.DefectRepository;
import ru.rt.rostelecom_tms.service.embedding.EmbeddingClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefectEmbeddingService {

    private static final int MAX_TOP_K = 10;

    private final EmbeddingClient embeddingClient;
    private final JdbcTemplate jdbcTemplate;
    private final DefectRepository defectRepository;

    @Transactional
    public void index(int defectId) {
        index(defectId, null);
    }

    @Transactional
    public void index(int defectId, String providerOverride) {
        Defect defect = defectRepository.findById(defectId)
                .orElseThrow(() -> new DefectNotFoundException("Couldn't find defect with id: " + defectId));

        float[] vector = toFloatArray(embeddingClient.embed(buildDefectText(defect), providerOverride));

        jdbcTemplate.update(
                """
                INSERT INTO defect_embeddings (defect_id, embedding, updated_at)
                VALUES (?, ?::vector, NOW())
                ON CONFLICT (defect_id)
                DO UPDATE SET embedding = EXCLUDED.embedding, updated_at = NOW()
                """,
                defect.getId(),
                new PGvector(vector)
        );
    }

    @Transactional
    public void indexAll(String providerOverride) {
        defectRepository.findAll().forEach(defect -> index(defect.getId(), providerOverride));
    }

    @Async("embeddingTaskExecutor")
    public CompletableFuture<Void> indexAllAsync(String providerOverride) {
        try {
            log.info("Started defect embeddings reindex-all (providerOverride={})", providerOverride);
            indexAll(providerOverride);
            log.info("Completed defect embeddings reindex-all (providerOverride={})", providerOverride);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to reindex all defect embeddings", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional
    public void delete(int defectId) {
        jdbcTemplate.update("DELETE FROM defect_embeddings WHERE defect_id = ?", defectId);
    }
    public List<SimilarDefectResult> findSimilar(int defectId, boolean onlySolved, int limit) {
        return findSimilar(defectId, onlySolved, limit, null);
    }

    public List<SimilarDefectResult> findSimilar(int defectId, boolean onlySolved, int limit, String providerOverride) {
        int normalizedLimit = normalizeLimit(limit);

        if (providerOverride != null) {
            log.warn(
                "findSimilar with providerOverride={} for defectId={}. " +
                "Stored vectors may have been indexed with a different provider. " +
                "Results may be inaccurate if providers differ.",
                providerOverride, defectId
            );
            Defect defect = defectRepository.findById(defectId)
                    .orElseThrow(() -> new DefectNotFoundException("Couldn't find defect with id: " + defectId));
            float[] vector = toFloatArray(embeddingClient.embed(buildDefectText(defect), providerOverride));
                return findSimilarByVector(vector, defectId, onlySolved, normalizedLimit);
        }

        float[] storedVector = loadStoredVector(defectId);
        if (storedVector == null) {
            Defect defect = defectRepository.findById(defectId)
                    .orElseThrow(() -> new DefectNotFoundException("Couldn't find defect with id: " + defectId));

            log.warn("Defect {} has no stored embedding. Attempting on-demand indexing.", defectId);
            try {
                index(defectId, null);
                storedVector = loadStoredVector(defectId);
            } catch (Exception e) {
                log.warn("On-demand indexing failed for defect {}: {}", defectId, e.getMessage());
            }

            if (storedVector == null) {
                log.warn("Falling back to on-the-fly vectorization for defect {}.", defectId);
                storedVector = toFloatArray(embeddingClient.embed(buildDefectText(defect)));
            }
        }

        List<SimilarDefectResult> results = findSimilarByVector(storedVector, defectId, onlySolved, normalizedLimit);
        if (!results.isEmpty()) {
            return results;
        }

        log.warn("No similar defects found for {}. Attempting full reindex and retry.", defectId);
        try {
            indexAll(null);
            float[] refreshedVector = loadStoredVector(defectId);
            if (refreshedVector == null) {
                refreshedVector = storedVector;
            }
            return findSimilarByVector(refreshedVector, defectId, onlySolved, normalizedLimit);
        } catch (Exception e) {
            log.warn("Full reindex fallback failed for defect {}: {}", defectId, e.getMessage());
            return results;
        }
    }

    float[] loadStoredVector(int defectId) {
        List<float[]> rows = jdbcTemplate.query(
                "SELECT embedding::text FROM defect_embeddings WHERE defect_id = ?",
                (rs, rowNum) -> parseVector(rs.getString(1)),
                defectId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public String buildDefectText(Defect defect) {
        StringBuilder sb = new StringBuilder(defect.getTitle());
        if (defect.getDescription() != null) {
            sb.append("\n").append(defect.getDescription());
        }
        return sb.toString();
    }

    private List<SimilarDefectResult> findSimilarByVector(
            float[] vector, int excludeDefectId, boolean onlySolved, int limit) {
        String sql = """
                SELECT de.defect_id,
                       d.case_id,
                       d.is_solved,
                       (1 - (de.embedding <=> ?::vector)) AS score
                FROM defect_embeddings de
                JOIN defects d ON d.id = de.defect_id
                WHERE de.defect_id <> ?
                  AND (? = FALSE OR d.is_solved = TRUE)
                ORDER BY de.embedding <=> ?::vector
                LIMIT ?
                """;

        PGvector pgv = new PGvector(vector);
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SimilarDefectResult(
                        rs.getInt("defect_id"),
                        rs.getInt("case_id"),
                        rs.getBoolean("is_solved"),
                        rs.getDouble("score")
                ),
                pgv, excludeDefectId, onlySolved, pgv, limit
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

    public record SimilarDefectResult(
            Integer defectId,
            Integer caseId,
            Boolean isSolved,
            Double score
    ) {}
}
