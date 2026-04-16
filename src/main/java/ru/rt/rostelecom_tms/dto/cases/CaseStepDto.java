package ru.rt.rostelecom_tms.dto.cases;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating/updating {@link ru.rt.rostelecom_tms.domain.cases.CaseStep}
 */
public record CaseStepDto(
        @NotNull @Min(1) Integer order,
        String title,
        @NotBlank String action,
        String expectedResult
) {
}
