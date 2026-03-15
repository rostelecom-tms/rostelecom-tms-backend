package ru.rt.rostelecom_tms.controller.runs;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.rt.rostelecom_tms.domain.runs.Run;
import ru.rt.rostelecom_tms.dto.runs.RunBulkDto;
import ru.rt.rostelecom_tms.dto.runs.RunCreateDto;
import ru.rt.rostelecom_tms.dto.runs.RunResponseDto;
import ru.rt.rostelecom_tms.service.runs.RunService;
import ru.rt.rostelecom_tms.util.mappers.RunMapper;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/runs")
@RequiredArgsConstructor
public class RunController {
    private final RunService runService;

    @GetMapping
    public List<RunResponseDto> getAll(
            @RequestParam(required = false) Integer planId,
            @RequestParam(required = false) Integer caseId,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(required = false) String statusSlug,
            @RequestParam(required = false) Integer executedBy,
            @RequestParam(required = false) Instant executedFrom,
            @RequestParam(required = false) Instant executedTo,
            @RequestParam(required = false) Integer groupId
    ) {

        List<Run> runsFromServiceResponse;

        if (planId != null) {
            runsFromServiceResponse = runService.findAllByPlanId(planId);
        } else if (caseId != null) {
           runsFromServiceResponse = runService.findAllByCaseId(caseId);
        } else if (statusId != null) {
            runsFromServiceResponse = runService.findAllByStatusId(statusId);
        } else if (statusSlug != null) {
            runsFromServiceResponse = runService.findAllByStatusSlug(statusSlug);
        } else if (executedBy != null) {
            runsFromServiceResponse = runService.findAllByExecutedBy(executedBy);
        } else if (executedFrom != null && executedTo != null) {
            runsFromServiceResponse = runService.findAllByExecutedFromAndTo(executedFrom, executedTo);
        } else if (groupId != null) {
            runsFromServiceResponse = runService.findAllByGroupId(groupId);
        } else {
            runsFromServiceResponse = runService.findAll();
        }

        return runsFromServiceResponse.stream().map(RunMapper::toRunResponseDto).toList();
    }

    @PostMapping
    public RunResponseDto create(@RequestBody @Valid RunCreateDto runCreateDto) {
        return RunMapper.toRunResponseDto(runService.createRun(RunMapper.toCreateRunCommand(runCreateDto)));
    }

    @PostMapping("/:bulk")
    public List<RunResponseDto> createBulk(@RequestBody @Valid RunBulkDto runBulkCreateDto) {
        return RunMapper.toCreateRunCommandsFromBulk(runBulkCreateDto).stream().map(runService::createRun).map(RunMapper::toRunResponseDto).toList();
    }
}
