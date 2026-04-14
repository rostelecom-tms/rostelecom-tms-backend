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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.dto.common.PageResponseDto;
import ru.rt.rostelecom_tms.dto.plans.PlanCreateDto;
import ru.rt.rostelecom_tms.dto.plans.PlanResponseDto;
import ru.rt.rostelecom_tms.dto.plans.PlanUpdateDto;
import ru.rt.rostelecom_tms.security.CurrentUserResolver;
import ru.rt.rostelecom_tms.service.plans.PlanService;
import ru.rt.rostelecom_tms.util.PaginationUtils;
import ru.rt.rostelecom_tms.util.mappers.PlanMapper;

import java.time.LocalDate;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final CurrentUserResolver currentUserResolver;

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public PageResponseDto<PlanResponseDto> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer responsibleUserId,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) LocalDate startDateFrom,
            @RequestParam(required = false) LocalDate startDateTo,
            @RequestParam(required = false) LocalDate endDateFrom,
            @RequestParam(required = false) LocalDate endDateTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        User caller = currentUserResolver.resolveOrThrow();
        return PaginationUtils.paginate(planService.findAllWithFilters(
                        name,
                        responsibleUserId,
                        projectId,
                        startDateFrom,
                        startDateTo,
                        endDateFrom,
                        endDateTo,
                        caller
                ).stream()
                .map(PlanMapper::toDto)
                .toList(), page, size);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public PlanResponseDto getOne(@PathVariable int id) {
        User caller = currentUserResolver.resolveOrThrow();
        return PlanMapper.toDto(planService.findOne(id, caller));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public PlanResponseDto create(@RequestBody @Valid PlanCreateDto dto) {
        User caller = currentUserResolver.resolveOrThrow();
        return PlanMapper.toDto(planService.create(PlanMapper.toCreateCommand(dto), caller));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}")
    public void update(@PathVariable int id, @RequestBody @Valid PlanUpdateDto dto) {
        User caller = currentUserResolver.resolveOrThrow();
        planService.update(id, PlanMapper.toUpdateCommand(dto), caller);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable int id) {
        User caller = currentUserResolver.resolveOrThrow();
        planService.delete(id, caller);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{id}/add-case/{caseId}")
    public void addCase(@PathVariable int id, @PathVariable int caseId) {
        User caller = currentUserResolver.resolveOrThrow();
        planService.addCase(id, caseId, caller);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}/remove-case/{caseId}")
    public void removeCase(@PathVariable int id, @PathVariable int caseId) {
        User caller = currentUserResolver.resolveOrThrow();
        planService.removeCase(id, caseId, caller);
    }
}
