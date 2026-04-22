package ru.rt.rostelecom_tms.controller.search;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.rt.rostelecom_tms.dto.rag.CaseSuggestByTextRequest;
import ru.rt.rostelecom_tms.dto.rag.CaseSuggestRequest;
import ru.rt.rostelecom_tms.dto.rag.DefectAnalysisRequest;
import ru.rt.rostelecom_tms.dto.rag.LogsAnalysisRequest;
import ru.rt.rostelecom_tms.service.embedding.EmbeddingProvider;
import ru.rt.rostelecom_tms.service.llm.LlmProvider;
import ru.rt.rostelecom_tms.service.rag.CaseRagService;
import ru.rt.rostelecom_tms.service.rag.DefectRagService;
import ru.rt.rostelecom_tms.service.rag.LogsAnalysisService;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final DefectRagService defectRagService;
    private final CaseRagService caseRagService;
    private final LogsAnalysisService logsAnalysisService;

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/defects/{id}/analysis")
    public DefectRagService.RagDefectAnalysis defectAnalysis(
            @PathVariable int id,
            @RequestBody @Valid DefectAnalysisRequest req
    ) {
        validateProviders(req.getEmbeddingProvider(), req.getLlmProvider());
        return defectRagService.analyzeDefect(
                id, req.getLimit(), req.isOnlySolved(),
                req.getEmbeddingProvider(), req.getLlmProvider()
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/cases/{id}/suggest")
    public CaseRagService.RagCaseSuggestion suggestForCase(
            @PathVariable int id,
            @RequestBody @Valid CaseSuggestRequest req
    ) {
        validateProviders(req.getEmbeddingProvider(), req.getLlmProvider());
        return caseRagService.suggestForCase(
                id, req.getLimit(),
                req.getEmbeddingProvider(), req.getLlmProvider()
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/cases/suggest")
    public CaseRagService.RagCaseSuggestion suggestByText(
            @RequestBody @Valid CaseSuggestByTextRequest req
    ) {
        validateProviders(req.getEmbeddingProvider(), req.getLlmProvider());
        return caseRagService.suggestForText(
                req.getQ(), req.getLimit(),
                req.getEmbeddingProvider(), req.getLlmProvider()
        );
    }

        @SecurityRequirement(name = "bearerAuth")
        @PreAuthorize("isAuthenticated()")
        @PostMapping("/logs/analysis")
        public LogsAnalysisService.LogsAnalysisResponse analyzeLogs(
            @RequestBody @Valid LogsAnalysisRequest req
        ) {
        validateProviders(null, req.getLlmProvider());
        return logsAnalysisService.analyze(req.getPrompt(), req.getLlmProvider());
        }

    private void validateProviders(String embeddingProvider, String llmProvider) {
        if (embeddingProvider != null) {
            try {
                EmbeddingProvider ep = EmbeddingProvider.from(embeddingProvider, null);
                if (ep == EmbeddingProvider.THIRD_AI) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "THIRD_AI embedding provider is not implemented yet");
                }
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown embedding provider: " + embeddingProvider + ". Supported: openai, ollama");
            }
        }
        if (llmProvider != null) {
            try {
                LlmProvider lp = LlmProvider.from(llmProvider, null);
                if (lp == LlmProvider.THIRD_AI) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "THIRD_AI LLM provider is not implemented yet");
                }
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown LLM provider: " + llmProvider + ". Supported: openai, ollama");
            }
        }
    }
}
