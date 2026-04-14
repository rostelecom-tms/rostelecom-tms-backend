package ru.rt.rostelecom_tms.service.projects;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.projects.Project;
import ru.rt.rostelecom_tms.domain.projects.ProjectMember;
import ru.rt.rostelecom_tms.domain.projects.exceptions.ProjectAccessDeniedException;
import ru.rt.rostelecom_tms.domain.projects.exceptions.ProjectAlreadyExistsException;
import ru.rt.rostelecom_tms.domain.projects.exceptions.ProjectMemberAlreadyExistsException;
import ru.rt.rostelecom_tms.domain.projects.exceptions.ProjectMemberNotFoundException;
import ru.rt.rostelecom_tms.domain.projects.exceptions.ProjectNotFoundException;
import ru.rt.rostelecom_tms.domain.users.RoleSlugs;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.repository.projects.ProjectMemberRepository;
import ru.rt.rostelecom_tms.repository.projects.ProjectRepository;
import ru.rt.rostelecom_tms.service.users.UserService;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserService userService;

    public record CreateProjectCommand(String name, String description) {}

    public record UpdateProjectCommand(String name, String description) {}

    public List<Project> findAll(User caller) {
        if (caller == null) {
            throw new ProjectAccessDeniedException("Authentication required");
        }

        return switch (caller.getRole().getSlug()) {
            case RoleSlugs.ADMIN -> projectRepository.findAllBy();
            case RoleSlugs.TEAMLEAD -> projectRepository.findDistinctByOwnerIdOrMembersUserId(caller.getId(), caller.getId());
            default -> projectRepository.findDistinctByMembersUserId(caller.getId());
        };
    }

    public Project findOne(int id, User caller) {
        Project project = projectRepository.findOneById(id)
                .orElseThrow(() -> new ProjectNotFoundException("Couldn't find project with id: " + id));
        checkReadAccess(project, caller);

        return project;
    }

    @Transactional
    public Project create(CreateProjectCommand cmd, User caller) {
        checkCanCreate(caller);
        if (projectRepository.existsByName(cmd.name())) {
            throw new ProjectAlreadyExistsException("Project with name '" + cmd.name() + "' already exists");
        }

        Project project = new Project();
        project.setName(cmd.name());
        project.setDescription(cmd.description());
        project.setOwner(caller);
        project.setCreatedAt(Instant.now());

        Project saved = projectRepository.save(project);
        return projectRepository.findOneById(saved.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Couldn't reload project after create, id: " + saved.getId()));
    }

    @Transactional
    public void update(int id, UpdateProjectCommand cmd, User caller) {
        Project project = projectRepository.findOneById(id)
                .orElseThrow(() -> new ProjectNotFoundException("Couldn't find project with id: " + id));
        checkWriteAccess(project, caller);

        if (cmd.name() != null) {
            if (!cmd.name().equals(project.getName()) && projectRepository.existsByName(cmd.name())) {
                throw new ProjectAlreadyExistsException("Project with name '" + cmd.name() + "' already exists");
            }
            project.setName(cmd.name());
        }

        if (cmd.description() != null) {
            project.setDescription(cmd.description());
        }

        projectRepository.save(project);
    }

    @Transactional
    public void delete(int id, User caller) {
        Project project = projectRepository.findOneById(id)
                .orElseThrow(() -> new ProjectNotFoundException("Couldn't find project with id: " + id));
        checkWriteAccess(project, caller);
        projectRepository.deleteById(id);
    }

    @Transactional
    public void addMember(int projectId, int userId, User caller) {
        Project project = projectRepository.findOneById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Couldn't find project with id: " + projectId));
        checkWriteAccess(project, caller);

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ProjectMemberAlreadyExistsException(
                    "User " + userId + " is already a member of project " + projectId
            );
        }

        User user = userService.findOne(userId);

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setAddedAt(Instant.now());
        projectMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(int projectId, int userId, User caller) {
        Project project = projectRepository.findOneById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Couldn't find project with id: " + projectId));
        checkWriteAccess(project, caller);

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ProjectMemberNotFoundException(
                        "User " + userId + " is not a member of project " + projectId
                ));
        projectMemberRepository.delete(member);
    }

    private void checkCanCreate(User caller) {
        if (caller == null) {
            throw new ProjectAccessDeniedException("Authentication required");
        }

        String slug = caller.getRole().getSlug();

        if (RoleSlugs.ADMIN.equals(slug))
            return;

        if (RoleSlugs.TEAMLEAD.equals(slug)) {
            if (!caller.isCanCreatePlans()) {
                throw new ProjectAccessDeniedException(
                        "Teamlead does not have permission to create projects"
                );
            }

            return;
        }
        throw new ProjectAccessDeniedException("Users cannot create projects");
    }

    private void checkReadAccess(Project project, User caller) {
        if (caller == null) {
            throw new ProjectAccessDeniedException("Authentication required");
        }

        String slug = caller.getRole().getSlug();

        if (RoleSlugs.ADMIN.equals(slug))
            return;

        boolean isOwner = project.getOwner().getId().equals(caller.getId());
        boolean isMember = project.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(caller.getId()));
        if (RoleSlugs.TEAMLEAD.equals(slug)) {
            if (!isOwner && !isMember) {
                throw new ProjectAccessDeniedException("Teamlead can only access owned or entrusted projects");
            }

            return;
        }
        if (!isMember) {
            throw new ProjectAccessDeniedException("Access to this project has not been granted");
        }
    }

    private void checkWriteAccess(Project project, User caller) {
        if (caller == null) {
            throw new ProjectAccessDeniedException("Authentication required");
        }
        String slug = caller.getRole().getSlug();
        if (RoleSlugs.ADMIN.equals(slug))
            return;

        boolean isOwner = project.getOwner().getId().equals(caller.getId());
        boolean isMember = project.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(caller.getId()));

        if (RoleSlugs.TEAMLEAD.equals(slug)) {
            if (!isOwner && !isMember) {
                throw new ProjectAccessDeniedException("Teamlead can only modify owned or entrusted projects");
            }

            return;
        }
        throw new ProjectAccessDeniedException("Users do not have write access to projects");
    }
}
