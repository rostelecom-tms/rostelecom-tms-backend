package ru.rt.rostelecom_tms.dto.rag;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class CaseSuggestRequest {

    @Min(1) @Max(10)
    private int limit = 5;

    private String embeddingProvider;

    private String llmProvider;

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public String getEmbeddingProvider() { return embeddingProvider; }
    public void setEmbeddingProvider(String embeddingProvider) { this.embeddingProvider = embeddingProvider; }

    public String getLlmProvider() { return llmProvider; }
    public void setLlmProvider(String llmProvider) { this.llmProvider = llmProvider; }
}
