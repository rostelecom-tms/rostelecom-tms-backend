package ru.rt.rostelecom_tms.service.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.CaseStep;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseNotFoundException;
import ru.rt.rostelecom_tms.repository.cases.CaseRepository;
import ru.rt.rostelecom_tms.service.llm.LlmClient;
import ru.rt.rostelecom_tms.service.search.CaseEmbeddingService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaseRagService {

    private final CaseRepository caseRepository;
    private final CaseEmbeddingService caseEmbeddingService;
    private final LlmClient llmClient;

    public RagCaseSuggestion suggestForText(
            String description,
            int limit,
            String embeddingProvider,
            String llmProvider
    ) {
        List<CaseEmbeddingService.SimilarCaseResult> similar =
                caseEmbeddingService.findSimilarByText(description, limit, embeddingProvider);

        List<Integer> similarIds = similar.stream()
                .map(CaseEmbeddingService.SimilarCaseResult::caseId)
                .toList();

        Map<Integer, Case> related = caseRepository.findAllByIdIn(similarIds).stream()
                .collect(Collectors.toMap(Case::getId, c -> c));

        String context = buildContext(description, similar, related);
        String systemPrompt = "You are a QA engineer assistant. Based on similar existing test cases, suggest a new test case or highlight gaps. Return plain text with sections: Suggested Title, Preconditions, Steps (numbered), Expected Result. Do not output reasoning or <think> tags.";
        String userPrompt = "New feature description and similar existing test cases:\n\n" + context;

        String answer = normalizeAnswer(llmClient.complete(systemPrompt, userPrompt, llmProvider));
        return new RagCaseSuggestion(similarIds, answer);
    }

    public RagCaseSuggestion suggestForCase(
            int caseId,
            int limit,
            String embeddingProvider,
            String llmProvider
    ) {
        Case base = caseRepository.findOneById(caseId)
                .orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + caseId));

        List<CaseEmbeddingService.SimilarCaseResult> similar =
                caseEmbeddingService.findSimilar(caseId, limit, embeddingProvider);

        List<Integer> similarIds = similar.stream()
                .map(CaseEmbeddingService.SimilarCaseResult::caseId)
                .toList();

        Map<Integer, Case> related = caseRepository.findAllByIdIn(similarIds).stream()
                .collect(Collectors.toMap(Case::getId, c -> c));


        String baseText = caseEmbeddingService.buildCaseText(base);
        String context = buildContext(baseText, similar, related);
        String systemPrompt = "You are a QA engineer assistant. Given a test case and similar ones, identify coverage gaps and suggest additional test scenarios. Return plain text with sections: Coverage Gaps, Suggested Additional Cases (numbered). Do not output reasoning or <think> tags.";
        String userPrompt = "Current test case and similar ones:\n\n" + context;

        String answer = normalizeAnswer(llmClient.complete(systemPrompt, userPrompt, llmProvider));
        return new RagCaseSuggestion(similarIds, answer);
    }

    private String buildContext(
            String baseText,
            List<CaseEmbeddingService.SimilarCaseResult> similar,
            Map<Integer, Case> related
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Input:\n").append(trim(baseText, 1500)).append("\n\n");
        sb.append("Similar existing test cases:\n");

        List<CaseEmbeddingService.SimilarCaseResult> ordered = similar.stream()
                .sorted(Comparator.comparing(CaseEmbeddingService.SimilarCaseResult::score).reversed())
                .toList();

        if (ordered.isEmpty()) {
            sb.append("- none\n");
            return sb.toString();
        }

        for (CaseEmbeddingService.SimilarCaseResult hit : ordered) {
            Case c = related.get(hit.caseId());
            if (c == null) continue;

            sb.append("- id: ").append(hit.caseId()).append("\n");
            sb.append("  title: ").append(trim(safe(c.getTitle()), 200)).append("\n");
            if (c.getDescription() != null) {
                sb.append("  description: ").append(trim(c.getDescription(), 400)).append("\n");
            }
            List<CaseStep> steps = c.getCaseSteps().stream()
                    .sorted(Comparator.comparingInt(CaseStep::getOrder))
                    .toList();
            if (!steps.isEmpty()) {
                sb.append("  steps:\n");
                steps.forEach(s -> sb.append("    ").append(s.getOrder())
                        .append(". ").append(trim(safe(s.getAction()), 150)).append("\n"));
            }
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

        int idx = indexOfIgnoreCase(sanitized, "Suggested Title");
        if (idx < 0) idx = indexOfIgnoreCase(sanitized, "Coverage Gaps");
        if (idx > 0) sanitized = sanitized.substring(idx).trim();

        return sanitized;
    }

    private int indexOfIgnoreCase(String source, String token) {
        return source.toLowerCase().indexOf(token.toLowerCase());
    }

    public record RagCaseSuggestion(
            List<Integer> relatedCaseIds,
            String answer
    ) {}
}