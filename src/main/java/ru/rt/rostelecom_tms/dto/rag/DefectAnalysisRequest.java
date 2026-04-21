package ru.rt.rostelecom_tms.dto.rag;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DefectAnalysisRequest {

    @Min(1) @Max(10)
    private int limit = 5;

    private boolean onlySolved = false;

    private String embeddingProvider;

    private String llmProvider;
}
