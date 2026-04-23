package ru.rt.rostelecom_tms.dto.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LogsAnalysisRequest {

    @NotNull
    private Integer defectId;

    @NotBlank
    private String prompt;

    private String llmProvider;

    private String llmModel;

    private boolean saveHistory = true;
}