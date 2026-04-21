package ru.rt.rostelecom_tms.controller.search;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.rt.rostelecom_tms.service.embedding.EmbeddingClient;
import ru.rt.rostelecom_tms.service.embedding.EmbeddingProvider;
import ru.rt.rostelecom_tms.service.search.CaseEmbeddingService;
import ru.rt.rostelecom_tms.service.search.DefectEmbeddingService;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final EmbeddingClient embeddingClient;
    private final CaseEmbeddingService caseEmbeddingService;
    private final DefectEmbeddingService defectEmbeddingService;

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/providers")
    public ProviderInfo providers() {
        return new ProviderInfo(embeddingClient.getDefaultProvider(), embeddingClient.getSupportedProviders());
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/cases/{id}/similar")
    public List<CaseEmbeddingService.SimilarCaseResult> similarCases(
            @PathVariable int id,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String provider
    ) {
        validateEmbeddingProvider(provider);
        return caseEmbeddingService.findSimilar(id, limit, provider);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/cases/similar")
    public List<CaseEmbeddingService.SimilarCaseResult> similarCasesByText(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String provider
    ) {
        validateEmbeddingProvider(provider);
        return caseEmbeddingService.findSimilarByText(q, limit, provider);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/defects/{id}/similar")
    public List<DefectEmbeddingService.SimilarDefectResult> similarDefects(
            @PathVariable int id,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "false") boolean onlySolved,
            @RequestParam(required = false) String provider
    ) {
        validateEmbeddingProvider(provider);
        return defectEmbeddingService.findSimilar(id, onlySolved, limit, provider);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/cases/{id}/reindex")
    public void reindexCase(
            @PathVariable int id,
            @RequestParam(required = false) String provider
    ) {
        validateEmbeddingProvider(provider);
        caseEmbeddingService.index(id, provider);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/cases/reindex-all")
    public void reindexAllCases(
            @RequestParam(required = false) String provider
    ) {
        validateEmbeddingProvider(provider);
        caseEmbeddingService.indexAllAsync(provider);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/defects/{id}/reindex")
    public void reindexDefect(
            @PathVariable int id,
            @RequestParam(required = false) String provider
    ) {
        validateEmbeddingProvider(provider);
        defectEmbeddingService.index(id, provider);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/defects/reindex-all")
    public void reindexAllDefects(
            @RequestParam(required = false) String provider
    ) {
        validateEmbeddingProvider(provider);
        defectEmbeddingService.indexAllAsync(provider);
    }

    private void validateEmbeddingProvider(String provider) {
        if (provider == null) return;
        try {
            EmbeddingProvider ep = EmbeddingProvider.from(provider, null);
            if (ep == EmbeddingProvider.THIRD_AI) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "THIRD_AI embedding provider is not implemented yet");
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown embedding provider: " + provider + ". Supported: openai, ollama");
        }
    }

    public record ProviderInfo(String defaultProvider, List<String> supportedProviders) {
    }
}
