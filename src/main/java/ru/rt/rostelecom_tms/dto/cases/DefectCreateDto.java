package ru.rt.rostelecom_tms.dto.cases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for {@link ru.rt.rostelecom_tms.domain.cases.Defect}
 */
public record DefectCreateDto(
        @NotNull Integer caseId,
        @NotBlank String title,
        String description
) {
}