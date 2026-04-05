package ru.rt.rostelecom_tms.dto.cases;

/**
 * DTO for {@link ru.rt.rostelecom_tms.domain.cases.CaseGroup}
 */
public record CaseGroupResponseDto(
        Integer id,
        String name,
        String slug,
        Integer projectId
) {
}
