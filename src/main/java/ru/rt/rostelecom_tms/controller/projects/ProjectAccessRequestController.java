package ru.rt.rostelecom_tms.controller.projects;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.dto.projects.ProjectAccessRequestCreateDto;
import ru.rt.rostelecom_tms.dto.projects.ProjectAccessRequestDecisionDto;
import ru.rt.rostelecom_tms.dto.projects.ProjectAccessRequestResponseDto;
import ru.rt.rostelecom_tms.security.CurrentUserResolver;
import ru.rt.rostelecom_tms.service.projects.ProjectAccessRequestService;
import ru.rt.rostelecom_tms.util.mappers.ProjectAccessRequestMapper;

import java.util.List;

@RestController
@RequestMapping("/project-access-requests")
@RequiredArgsConstructor
public class ProjectAccessRequestController {

    private final ProjectAccessRequestService projectAccessRequestService;
    private final CurrentUserResolver currentUserResolver;

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ProjectAccessRequestResponseDto create(@RequestBody @Valid ProjectAccessRequestCreateDto dto) {
        User caller = currentUserResolver.resolveOrThrow();
        return ProjectAccessRequestMapper.toDto(
                projectAccessRequestService.create(dto.projectId(), dto.comment(), caller)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEAMLEAD')")
    @GetMapping("/inbox")
    public List<ProjectAccessRequestResponseDto> inbox() {
        User caller = currentUserResolver.resolveOrThrow();
        return projectAccessRequestService.inbox(caller)
                .stream()
                .map(ProjectAccessRequestMapper::toDto)
                .toList();
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEAMLEAD')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{id}/approve")
    public void approve(@PathVariable Integer id, @RequestBody(required = false) ProjectAccessRequestDecisionDto dto) {
        User caller = currentUserResolver.resolveOrThrow();
        String comment = dto == null ? null : dto.comment();
        projectAccessRequestService.approve(id, comment, caller);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEAMLEAD')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{id}/reject")
    public void reject(@PathVariable Integer id, @RequestBody(required = false) ProjectAccessRequestDecisionDto dto) {
        User caller = currentUserResolver.resolveOrThrow();
        String comment = dto == null ? null : dto.comment();
        projectAccessRequestService.reject(id, comment, caller);
    }
}
