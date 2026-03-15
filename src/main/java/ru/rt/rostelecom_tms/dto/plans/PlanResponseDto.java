package ru.rt.rostelecom_tms.dto.plans;

import ru.rt.rostelecom_tms.dto.cases.CaseResponseDto;

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
        PlanResponsibleUserDto responsibleUser,
        Instant createdAt,
        List<CaseResponseDto> cases
) {}
