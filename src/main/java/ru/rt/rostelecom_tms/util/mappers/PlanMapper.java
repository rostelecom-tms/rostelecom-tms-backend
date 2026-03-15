package ru.rt.rostelecom_tms.util.mappers;

import ru.rt.rostelecom_tms.domain.plans.Plan;
import ru.rt.rostelecom_tms.dto.cases.CaseResponseDto;
import ru.rt.rostelecom_tms.dto.plans.*;
import ru.rt.rostelecom_tms.service.plans.PlanService;

import java.util.Collections;
import java.util.List;

public class PlanMapper {

    public static PlanService.CreatePlanCommand toCreateCommand(PlanCreateDto dto) {
        return new PlanService.CreatePlanCommand(
                dto.name(),
                dto.introduction(),
                dto.approach(),
                dto.startDate(),
                dto.endDate(),
                dto.responsibleUserId()
        );
    }

    public static PlanService.UpdatePlanCommand toUpdateCommand(PlanUpdateDto dto) {
        return new PlanService.UpdatePlanCommand(
                dto.name(),
                dto.introduction(),
                dto.approach(),
                dto.startDate(),
                dto.endDate(),
                dto.responsibleUserId()
        );
    }

    public static PlanResponseDto toDto(Plan plan) {
        PlanResponsibleUserDto responsibleUser = plan.getResponsibleUser() == null ? null
                : new PlanResponsibleUserDto(
                        plan.getResponsibleUser().getId(),
                        plan.getResponsibleUser().getUsername(),
                        plan.getResponsibleUser().getEmail()
                );

        List<CaseResponseDto> cases = plan.getCases() == null
                ? Collections.emptyList()
                : plan.getCases().stream()
                        .map(CaseMapper::toDto)
                        .toList();

        return new PlanResponseDto(
                plan.getId(),
                plan.getName(),
                plan.getIntroduction(),
                plan.getApproach(),
                plan.getStartDate(),
                plan.getEndDate(),
                responsibleUser,
                plan.getCreatedAt(),
                cases
        );
    }
}
