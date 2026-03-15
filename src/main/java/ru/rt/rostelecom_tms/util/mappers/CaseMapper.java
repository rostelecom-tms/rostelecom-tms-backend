package ru.rt.rostelecom_tms.util.mappers;

import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.CaseGroup;
import ru.rt.rostelecom_tms.domain.cases.CaseStep;
import ru.rt.rostelecom_tms.dto.cases.*;
import ru.rt.rostelecom_tms.service.cases.CaseService;
import ru.rt.rostelecom_tms.service.cases.CaseStepService;

import static ru.rt.rostelecom_tms.service.cases.CaseStepService.StepCommand;

import java.util.Comparator;
import java.util.Collections;
import java.util.List;

public class CaseMapper {

    public static CaseService.CreateCaseCommand toCreateCommand(CaseCreateDto dto) {
        return new CaseService.CreateCaseCommand(
                dto.title(),
                dto.groupId(),
                dto.description(),
                dto.preconditions(),
                dto.postconditions(),
                dto.steps() == null ? null : dto.steps().stream()
                        .map(s -> new StepCommand(s.order(), s.title(), s.action(), s.expectedResult()))
                        .toList()
        );
    }

    public static StepCommand toCreateCommand(CaseStepDto dto) {
        return new StepCommand(
                dto.order(),
                dto.title(),
                dto.action(),
                dto.expectedResult()
        );
    }

    public static CaseService.UpdateCaseCommand toUpdateCommand(CaseUpdateDto dto) {
        return new CaseService.UpdateCaseCommand(
                dto.title(),
                dto.groupId(),
                dto.description(),
                dto.preconditions(),
                dto.postconditions(),
                dto.steps() == null ? null : dto.steps().stream()
                        .map(s -> new StepCommand(s.order(), s.title(), s.action(), s.expectedResult()))
                        .toList()
        );
    }

    public static CaseStepService.UpdateCaseStepCommand toUpdateCommand(CaseStepDto dto) {
        return new CaseStepService.UpdateCaseStepCommand(
                dto.order(),
                dto.title(),
                dto.action(),
                dto.expectedResult()
        );
    }

    public static CaseSimpleResponseDto toSimpleDto(Case c) {
        return new CaseSimpleResponseDto(c.getId(), c.getTitle());
    }

    public static CaseGroupResponseDto toDto(CaseGroup group) {
        return new CaseGroupResponseDto(
                group.getId(),
                group.getName(),
                group.getSlug()
        );
    }

    public static CaseStepResponseDto toDto(CaseStep step) {
        return new CaseStepResponseDto(
                step.getId(),
                step.getOrder(),
                step.getTitle(),
                step.getAction(),
                step.getExpectedResult()
        );
    }

    public static CaseResponseDto toDto(Case c) {
        List<CaseStepResponseDto> steps = c.getCaseSteps() == null
                ? Collections.emptyList()
                : c.getCaseSteps().stream()
                        .sorted(Comparator.comparing(CaseStep::getOrder))
                        .map(CaseMapper::toDto)
                        .toList();

        return new CaseResponseDto(
                c.getId(),
                c.getTitle(),
                toDto(c.getGroup()),
                c.getDescription(),
                c.getPreconditions(),
                c.getPostconditions(),
                c.getCreatedAt(),
                steps
        );
    }
}
