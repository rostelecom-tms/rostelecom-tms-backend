package ru.rt.rostelecom_tms.dto.plans;

/**
 * DTO for {@link ru.rt.rostelecom_tms.domain.plans.PlansCase}
 */
public record PlansCaseResponseDto(
        Integer id,
        PlansCaseCaseDto caseField
) {}
