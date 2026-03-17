package ru.rt.rostelecom_tms.util.mappers;

import ru.rt.rostelecom_tms.domain.runs.Run;
import ru.rt.rostelecom_tms.dto.runs.RunBulkDto;
import ru.rt.rostelecom_tms.dto.runs.RunCreateDto;
import ru.rt.rostelecom_tms.dto.runs.RunResponseDto;
import ru.rt.rostelecom_tms.service.runs.RunService;

import java.util.List;

public class RunMapper {

    public static RunResponseDto toRunResponseDto(Run run) {
        return new RunResponseDto(
                run.getId(),
                run.getCaseField().getId(),
                run.getPlan().getId(),
                run.getStatus().getName(),
                run.getStatus().getSlug(),
                run.getExecutedBy().getId(),
                run.getExecutedAt()
        );
    }

    public static RunService.CreateRunCommand toCreateRunCommand(RunCreateDto runCreateDto) {
        return new RunService.CreateRunCommand(
                runCreateDto.planId(),
                runCreateDto.caseId(),
                runCreateDto.statusId(),
                runCreateDto.executedBy(),
                runCreateDto.executedAt()
        );
    }

    public static List<RunService.CreateRunCommandFromBulk> toCreateRunCommandsFromBulk(RunBulkDto runBulkCreateDto) {
        Integer planId = runBulkCreateDto.planId();
        Integer executedBy = runBulkCreateDto.executedBy();

        return runBulkCreateDto.results().stream()
                .map(resultDto -> new RunService.CreateRunCommandFromBulk(
                        planId,
                        resultDto.caseId(),
                        resultDto.statusSlug(),
                        executedBy,
                        resultDto.executedAt()
                ))
                .toList();
    }
}
