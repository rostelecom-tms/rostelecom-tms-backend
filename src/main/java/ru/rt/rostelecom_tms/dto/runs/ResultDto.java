package ru.rt.rostelecom_tms.dto.runs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.rt.rostelecom_tms.domain.runs.Run;

import java.time.Instant;

/**
 * DTO for {@link Run}
 */
public record ResultDto(
        @NotNull Integer caseId,
        @NotBlank String statusSlug,
        @NotNull Instant executedAt
) {}
