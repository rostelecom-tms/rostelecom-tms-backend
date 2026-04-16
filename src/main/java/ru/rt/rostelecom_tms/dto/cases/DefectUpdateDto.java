package ru.rt.rostelecom_tms.dto.cases;

/**
 * DTO for {@link ru.rt.rostelecom_tms.domain.cases.Defect}
 */
public record DefectUpdateDto(
        String title,
        String description,
        Boolean isSolved
) {
}