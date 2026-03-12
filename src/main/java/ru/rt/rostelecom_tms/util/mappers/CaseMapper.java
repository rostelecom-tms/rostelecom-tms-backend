package ru.rt.rostelecom_tms.util.mappers;

import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.CaseGroup;
import ru.rt.rostelecom_tms.domain.cases.CaseStep;
import ru.rt.rostelecom_tms.dto.cases.CaseGroupResponseDto;
import ru.rt.rostelecom_tms.dto.cases.CaseResponseDto;
import ru.rt.rostelecom_tms.dto.cases.CaseStepResponseDto;

import java.util.Comparator;
import java.util.Collections;
import java.util.List;

public class CaseMapper {

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
