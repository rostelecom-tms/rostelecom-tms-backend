package ru.rt.rostelecom_tms.dto.runs;

import ru.rt.rostelecom_tms.domain.runs.Run;
import ru.rt.rostelecom_tms.domain.users.User;

import java.time.Instant;

/**
 * DTO for {@link Run}
 */
public record RunResponseDto(
        Integer id,
        Integer caseId,
        Integer planId,
        String statusName,
        String statusSlug,
        Integer executedBy,
        Instant executedAt
) {}