package ru.rt.rostelecom_tms.controller.plans;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.rt.rostelecom_tms.dto.plans.PlansCaseCreateDto;
import ru.rt.rostelecom_tms.dto.plans.PlansCaseResponseDto;
import ru.rt.rostelecom_tms.service.plans.PlansCaseService;
import ru.rt.rostelecom_tms.util.mappers.PlanMapper;

import java.util.List;

@RestController
@RequestMapping("/plans/{planId}/cases")
@RequiredArgsConstructor
public class PlansCaseController {

    private final PlansCaseService plansCaseService;

    @GetMapping
    public List<PlansCaseResponseDto> getAllByPlan(@PathVariable int planId) {
        return plansCaseService.findAllByPlan(planId).stream()
                .map(PlanMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public PlansCaseResponseDto getOne(@PathVariable int planId, @PathVariable int id) {
        return PlanMapper.toDto(plansCaseService.findOne(planId, id));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public PlansCaseResponseDto create(@PathVariable int planId, @RequestBody @Valid PlansCaseCreateDto dto) {
        return PlanMapper.toDto(plansCaseService.create(PlanMapper.toCreateCommand(planId, dto)));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable int planId, @PathVariable int id) {
        plansCaseService.delete(planId, id);
    }
}
