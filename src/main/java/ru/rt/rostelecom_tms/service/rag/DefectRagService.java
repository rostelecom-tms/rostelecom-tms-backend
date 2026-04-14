package ru.rt.rostelecom_tms.service.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rt.rostelecom_tms.domain.cases.Defect;
import ru.rt.rostelecom_tms.domain.cases.exceptions.DefectNotFoundException;
import ru.rt.rostelecom_tms.repository.cases.DefectRepository;
import ru.rt.rostelecom_tms.service.llm.LlmClient;
import ru.rt.rostelecom_tms.service.search.DefectEmbeddingService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefectRagService {

    private final DefectRepository defectRepository;
    private final DefectEmbeddingService defectEmbeddingService;
    private final LlmClient llmClient;

    public RagDefectAnalysis analyzeDefect(
            int defectId,
            int limit,
            boolean onlySolved,
            String embeddingProvider,
            String llmProvider
    ) {
        Defect baseDefect = defectRepository.findById(defectId)
                .orElseThrow(() -> new DefectNotFoundException("Couldn't find defect with id: " + defectId));

        List<DefectEmbeddingService.SimilarDefectResult> similar = defectEmbeddingService
                .findSimilar(defectId, onlySolved, limit, embeddingProvider);

        List<Integer> similarIds = similar.stream()
                .map(DefectEmbeddingService.SimilarDefectResult::defectId)
                .toList();

        Map<Integer, Defect> related = defectRepository.findAllById(similarIds).stream()
                .collect(Collectors.toMap(Defect::getId, d -> d));

        String context = buildContext(baseDefect, similar, related);
        String systemPrompt = "You are a QA defect triage assistant. Return only final answer in plain text with sections: Summary, Probable Cause, Recommended Actions. Do not output reasoning, chain-of-thought, analysis steps, or <think> tags.";
        String userPrompt = "Analyze the current defect using similar historical defects:\n\n" + context;

        String answer = normalizeAnswer(llmClient.complete(systemPrompt, userPrompt, llmProvider));
        return new RagDefectAnalysis(baseDefect.getId(), similarIds, answer);
    }

    private String buildContext(
            Defect base,
            List<DefectEmbeddingService.SimilarDefectResult> similar,
            Map<Integer, Defect> related
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current defect:\n");
        sb.append("- title: ").append(safe(base.getTitle())).append("\n");
        sb.append("- description: ").append(trim(safe(base.getDescription()), 1200)).append("\n\n");
        sb.append("Similar defects:\n");

        List<DefectEmbeddingService.SimilarDefectResult> ordered = similar.stream()
                .sorted(Comparator.comparing(DefectEmbeddingService.SimilarDefectResult::score).reversed())
                .toList();

        if (ordered.isEmpty()) {
            sb.append("- none\n");
            return sb.toString();
        }

        for (DefectEmbeddingService.SimilarDefectResult hit : ordered) {
            Defect d = related.get(hit.defectId());
            if (d == null) continue;

            sb.append("- solved: ").append(hit.isSolved()).append("\n");
            sb.append("  title: ").append(trim(safe(d.getTitle()), 200)).append("\n");
            sb.append("  description: ").append(trim(safe(d.getDescription()), 500)).append("\n");
        }

        return sb.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String normalizeAnswer(String raw) {
        if (raw == null || raw.isBlank()) return raw;

        String sanitized = raw
                .replaceAll("(?is)<think>.*?</think>", "")
                .replaceAll("(?im)^thinking\\.\\.\\..*$", "")
                .replaceAll("(?im)^\\.\\.\\.done thinking\\..*$", "")
                .trim();

        int summaryIndex = indexOfIgnoreCase(sanitized, "Summary");
        if (summaryIndex > 0) {
            sanitized = sanitized.substring(summaryIndex).trim();
        }

        return sanitized;
    }

    private int indexOfIgnoreCase(String source, String token) {
        return source.toLowerCase().indexOf(token.toLowerCase());
    }

    public record RagDefectAnalysis(
            Integer defectId,
            List<Integer> relatedDefectIds,
            String answer
    ) {}
}
