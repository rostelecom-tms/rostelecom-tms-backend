package ru.rt.rostelecom_tms.dto.cases;

import java.time.Instant;

/**
 * DTO for {@link ru.rt.rostelecom_tms.domain.cases.Defect}
 */
public record DefectResponseDto(
        Integer id,
        Integer caseId,
        String caseTitle,
        String title,
        String description,
        Boolean isSolved,
        Instant createdAt
) {
}
