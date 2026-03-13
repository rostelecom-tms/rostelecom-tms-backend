package ru.rt.rostelecom_tms.dto.plans;

import jakarta.validation.constraints.NotNull;

public record PlansCaseCreateDto(
        @NotNull Integer caseId
) {}
