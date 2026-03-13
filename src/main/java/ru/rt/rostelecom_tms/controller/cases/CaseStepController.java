package ru.rt.rostelecom_tms.controller.cases;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.rt.rostelecom_tms.dto.cases.CaseStepDto;
import ru.rt.rostelecom_tms.dto.cases.CaseStepResponseDto;
import ru.rt.rostelecom_tms.service.cases.CaseStepService;
import ru.rt.rostelecom_tms.util.mappers.CaseMapper;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CaseStepController {

    private final CaseStepService caseStepService;

    @GetMapping("/case/{caseId}/steps")
    public List<CaseStepResponseDto> getCaseSteps(@PathVariable int caseId) {
        return caseStepService.findAllByCaseId(caseId).stream()
                .map(CaseMapper::toDto)
                .toList();
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/case/{caseId}/steps")
    public List<CaseStepResponseDto> create(@PathVariable int caseId, @RequestBody @Valid List<CaseStepDto> dto) {
        return caseStepService.createCaseSteps(caseId, dto.stream()
                .map(CaseMapper::toCreateCommand)
                .toList())
                .stream()
                .map(CaseMapper::toDto)
                .toList();
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/case-steps/{caseStepId}")
    public void update(@PathVariable int caseStepId, @RequestBody @Valid CaseStepDto dto) {
        caseStepService.updateCaseSteps(caseStepId, CaseMapper.toUpdateCommand(dto));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/case-steps/{caseStepId}")
    public void delete(@PathVariable int caseStepId) {
        caseStepService.deleteCaseSteps(caseStepId);
    }
}
