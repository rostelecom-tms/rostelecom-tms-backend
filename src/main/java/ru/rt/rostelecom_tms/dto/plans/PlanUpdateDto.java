package ru.rt.rostelecom_tms.dto.plans;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO for updating {@link ru.rt.rostelecom_tms.domain.plans.Plan}
 */
public record PlanUpdateDto(
        @Size(min = 1, max = 500) String name,
        String introduction,
        String approach,
        LocalDate startDate,
        LocalDate endDate,
        Integer responsibleUserId
) {}
