package ru.rt.rostelecom_tms.dto.cases;

import java.time.Instant;
import java.util.List;

/**
 * DTO for {@link ru.rt.rostelecom_tms.domain.cases.Case}
 */
public record CaseResponseDto(
        Integer id,
        String title,
        CaseGroupResponseDto group,
        String description,
        String preconditions,
        String postconditions,
        Instant createdAt,
        List<CaseStepResponseDto> steps
) {
}
