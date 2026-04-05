package ru.rt.rostelecom_tms.controller.projects;

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
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.dto.projects.ProjectCreateDto;
import ru.rt.rostelecom_tms.dto.projects.ProjectMemberDto;
import ru.rt.rostelecom_tms.dto.projects.ProjectResponseDto;
import ru.rt.rostelecom_tms.dto.projects.ProjectUpdateDto;
import ru.rt.rostelecom_tms.security.CurrentUserResolver;
import ru.rt.rostelecom_tms.service.projects.ProjectService;
import ru.rt.rostelecom_tms.util.mappers.ProjectMapper;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUserResolver currentUserResolver;

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<ProjectResponseDto> getAll() {
        User caller = currentUserResolver.resolveOrThrow();
        return projectService.findAll(caller).stream()
                .map(ProjectMapper::toDto)
                .toList();
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ProjectResponseDto getOne(@PathVariable int id) {
        User caller = currentUserResolver.resolveOrThrow();
        return ProjectMapper.toDto(projectService.findOne(id, caller));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEAMLEAD')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ProjectResponseDto create(@RequestBody @Valid ProjectCreateDto dto) {
        User caller = currentUserResolver.resolveOrThrow();
        return ProjectMapper.toDto(projectService.create(
                new ProjectService.CreateProjectCommand(dto.name(), dto.description()),
                caller
        ));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEAMLEAD')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}")
    public void update(@PathVariable int id, @RequestBody @Valid ProjectUpdateDto dto) {
        User caller = currentUserResolver.resolveOrThrow();
        projectService.update(id, new ProjectService.UpdateProjectCommand(dto.name(), dto.description()), caller);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEAMLEAD')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable int id) {
        User caller = currentUserResolver.resolveOrThrow();
        projectService.delete(id, caller);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEAMLEAD')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{id}/members")
    public void addMember(@PathVariable int id, @RequestBody @Valid ProjectMemberDto dto) {
        User caller = currentUserResolver.resolveOrThrow();
        projectService.addMember(id, dto.userId(), caller);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEAMLEAD')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}/members/{userId}")
    public void removeMember(@PathVariable int id, @PathVariable int userId) {
        User caller = currentUserResolver.resolveOrThrow();
        projectService.removeMember(id, userId, caller);
    }
}
