package ru.rt.rostelecom_tms.dto.cases;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO for creating {@link ru.rt.rostelecom_tms.domain.cases.Case}
 */
public record CaseCreateDto(
        @NotBlank @Size(min = 1, max = 500) String title,
        @NotNull Integer groupId,
        String description,
        String preconditions,
        String postconditions,
        @Size(max = 30) List<@NotBlank @Size(max = 50) String> tags,
        @Valid List<CaseStepDto> steps
) {
}
