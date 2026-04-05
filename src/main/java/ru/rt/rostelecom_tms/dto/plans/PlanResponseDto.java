package ru.rt.rostelecom_tms.dto.plans;

import ru.rt.rostelecom_tms.dto.cases.CaseSimpleResponseDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for {@link ru.rt.rostelecom_tms.domain.plans.Plan}
 */
public record PlanResponseDto(
        Integer id,
        String name,
        String introduction,
        String approach,
        LocalDate startDate,
        LocalDate endDate,
        Integer projectId,
        PlanResponsibleUserDto responsibleUser,
        Instant createdAt,
        List<CaseSimpleResponseDto> cases
) {}
