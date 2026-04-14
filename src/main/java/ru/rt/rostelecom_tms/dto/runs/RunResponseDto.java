package ru.rt.rostelecom_tms.dto.runs;

import ru.rt.rostelecom_tms.domain.runs.Run;

import java.time.Instant;

/**
 * DTO for {@link Run}
 */
public record RunResponseDto(
        Integer id,
        Integer caseId,
        String caseTitle,
        Integer planId,
        String planName,
        Integer statusId,
        String statusName,
        String statusSlug,
        Integer executedBy,
        String executedByUsername,
        String executedByEmail,
        Instant executedAt
) {}
