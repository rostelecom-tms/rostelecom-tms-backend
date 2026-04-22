package ru.rt.rostelecom_tms.dto.rag;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogsAnalysisRequest {

    @NotBlank
    private String prompt;

    private String llmProvider;
}