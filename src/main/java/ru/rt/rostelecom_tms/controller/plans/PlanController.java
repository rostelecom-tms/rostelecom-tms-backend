package ru.rt.rostelecom_tms.controller.plans;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.rt.rostelecom_tms.dto.plans.PlanCreateDto;
import ru.rt.rostelecom_tms.dto.plans.PlanResponseDto;
import ru.rt.rostelecom_tms.dto.plans.PlanUpdateDto;
import ru.rt.rostelecom_tms.service.plans.PlanService;
import ru.rt.rostelecom_tms.util.mappers.PlanMapper;

import java.util.List;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public List<PlanResponseDto> getAll() {
        return planService.findAll().stream()
                .map(PlanMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public PlanResponseDto getOne(@PathVariable int id) {
        return PlanMapper.toDto(planService.findOne(id));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public PlanResponseDto create(@RequestBody @Valid PlanCreateDto dto) {
        return PlanMapper.toDto(planService.create(PlanMapper.toCreateCommand(dto)));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}")
    public void update(@PathVariable int id, @RequestBody @Valid PlanUpdateDto dto) {
        planService.update(id, PlanMapper.toUpdateCommand(dto));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable int id) {
        planService.delete(id);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{id}/add-case/{caseId}")
    public void addCase(@PathVariable int id, @PathVariable int caseId) {
        planService.addCase(id, caseId);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}/remove-case/{caseId}")
    public void removeCase(@PathVariable int id, @PathVariable int caseId) {
        planService.removeCase(id, caseId);
    }
}
