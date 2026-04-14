package ru.rt.rostelecom_tms.dto.cases;

/**
 * Short DTO for {@link ru.rt.rostelecom_tms.domain.cases.Case}.
 */
public record CaseSimpleResponseDto(
        Integer id,
        String title,
        Integer groupId
) {}
