package ru.rt.rostelecom_tms.controller.runs;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.rt.rostelecom_tms.dto.runs.RunBulkDto;
import ru.rt.rostelecom_tms.dto.runs.RunResponseDto;
import ru.rt.rostelecom_tms.service.runs.RunIngestionAuthService;
import ru.rt.rostelecom_tms.service.runs.RunService;
import ru.rt.rostelecom_tms.util.mappers.RunMapper;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RunIngestionController {

    private final RunService runService;
    private final RunIngestionAuthService runIngestionAuthService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping({"/integrations/runs", "/webhooks/runs"})
    public List<RunResponseDto> ingestBulk(
            @RequestBody @Valid RunBulkDto runBulkCreateDto,
            @RequestHeader(value = "X-Runs-Token", required = false) String tokenHeader,
            @RequestParam(value = "token", required = false) String tokenQuery
    ) {
        runIngestionAuthService.assertAuthorized(resolveToken(tokenHeader, tokenQuery));
        return runService.createRunsBulkFromIntegration(RunMapper.toCreateRunCommandsFromBulk(runBulkCreateDto))
                .stream()
                .map(RunMapper::toRunResponseDto)
                .toList();
    }

    private String resolveToken(String tokenHeader, String tokenQuery) {
        if (tokenHeader != null && !tokenHeader.isBlank()) {
            return tokenHeader;
        }
        return tokenQuery;
    }
}
