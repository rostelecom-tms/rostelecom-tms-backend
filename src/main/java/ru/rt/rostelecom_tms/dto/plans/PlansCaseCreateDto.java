package ru.rt.rostelecom_tms.dto.plans;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a {@link ru.rt.rostelecom_tms.domain.plans.PlansCase}
 */
public record PlansCaseCreateDto(
        @NotNull Integer caseId
) {}
