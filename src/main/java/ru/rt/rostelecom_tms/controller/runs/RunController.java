package ru.rt.rostelecom_tms.controller.runs;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.dto.runs.RunBulkDto;
import ru.rt.rostelecom_tms.dto.runs.RunCreateDto;
import ru.rt.rostelecom_tms.dto.runs.RunResponseDto;
import ru.rt.rostelecom_tms.security.CurrentUserResolver;
import ru.rt.rostelecom_tms.service.runs.RunService;
import ru.rt.rostelecom_tms.util.mappers.RunMapper;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/runs")
@RequiredArgsConstructor
public class RunController {
    private final RunService runService;
    private final CurrentUserResolver currentUserResolver;

    @PreAuthorize("isAuthenticated()")
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
            User caller = currentUserResolver.resolveOrThrow();
        return runService.findAllWithProbableFilters(
                planId,
                caseId,
                statusId,
                statusSlug,
                executedBy,
                executedFrom,
                executedTo,
                groupId,
                caller
        ).stream().map(RunMapper::toRunResponseDto).toList();

    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public RunResponseDto create(@RequestBody @Valid RunCreateDto runCreateDto) {
        User caller = currentUserResolver.resolveOrThrow();
        return RunMapper.toRunResponseDto(runService.createRun(RunMapper.toCreateRunCommand(runCreateDto), caller));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/bulk")
    public List<RunResponseDto> createBulk(@RequestBody @Valid RunBulkDto runBulkCreateDto) {
        User caller = currentUserResolver.resolveOrThrow();
        return runService.createRunsBulk(RunMapper.toCreateRunCommandsFromBulk(runBulkCreateDto), caller).stream().map(RunMapper::toRunResponseDto).toList();
    }
}
