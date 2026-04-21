package ru.rt.rostelecom_tms.dto.rag;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CaseSuggestRequest {

    @Min(1) @Max(10)
    private int limit = 5;

    private String embeddingProvider;

    private String llmProvider;
}
