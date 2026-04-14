package ru.rt.rostelecom_tms.dto.rag;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class DefectAnalysisRequest {

    @Min(1) @Max(10)
    private int limit = 5;

    private boolean onlySolved = false;

    private String embeddingProvider;

    private String llmProvider;

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public boolean isOnlySolved() { return onlySolved; }
    public void setOnlySolved(boolean onlySolved) { this.onlySolved = onlySolved; }

    public String getEmbeddingProvider() { return embeddingProvider; }
    public void setEmbeddingProvider(String embeddingProvider) { this.embeddingProvider = embeddingProvider; }

    public String getLlmProvider() { return llmProvider; }
    public void setLlmProvider(String llmProvider) { this.llmProvider = llmProvider; }
}
