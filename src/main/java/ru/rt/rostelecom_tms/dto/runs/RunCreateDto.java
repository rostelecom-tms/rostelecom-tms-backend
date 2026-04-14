package ru.rt.rostelecom_tms.dto.runs;

import jakarta.validation.constraints.NotNull;
import ru.rt.rostelecom_tms.domain.runs.Run;

import java.time.Instant;

/**
 * DTO for {@link Run}
 */
public record RunCreateDto(
        @NotNull Integer planId,
        @NotNull Integer caseId,
        @NotNull Integer statusId,
        Integer executedBy,
        @NotNull Instant executedAt
) {}
