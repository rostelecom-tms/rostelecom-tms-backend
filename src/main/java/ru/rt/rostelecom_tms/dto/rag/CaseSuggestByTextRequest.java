package ru.rt.rostelecom_tms.dto.rag;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CaseSuggestByTextRequest {

    @NotBlank
    private String q;

    @Min(1) @Max(10)
    private int limit = 5;

    private String embeddingProvider;

    private String llmProvider;

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public String getEmbeddingProvider() { return embeddingProvider; }
    public void setEmbeddingProvider(String embeddingProvider) { this.embeddingProvider = embeddingProvider; }

    public String getLlmProvider() { return llmProvider; }
    public void setLlmProvider(String llmProvider) { this.llmProvider = llmProvider; }
}
