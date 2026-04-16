package ru.rt.rostelecom_tms.dto.cases;

/**
 * DTO for {@link ru.rt.rostelecom_tms.domain.cases.CaseStep}
 */
public record CaseStepResponseDto(
        Integer id,
        Integer order,
        String title,
        String action,
        String expectedResult
) {
}
