package ru.rt.rostelecom_tms.service.cases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.cases.CaseGroup;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseGroupAlreadyExistsException;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseGroupNotDeletableException;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseGroupNotFoundException;
import ru.rt.rostelecom_tms.domain.projects.Project;
import ru.rt.rostelecom_tms.domain.projects.exceptions.ProjectNotFoundException;
import ru.rt.rostelecom_tms.domain.users.RoleSlugs;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.repository.cases.CaseRepository;
import ru.rt.rostelecom_tms.repository.cases.CaseGroupRepository;
import ru.rt.rostelecom_tms.repository.projects.ProjectMemberRepository;
import ru.rt.rostelecom_tms.repository.projects.ProjectRepository;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CaseGroupService {

    private final CaseGroupRepository caseGroupRepository;
    private final CaseRepository caseRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public record CreateGroupCommand(String name, String slug, Integer projectId) {
    }

    public record UpdateGroupCommand(String name, String slug, Integer projectId) {
    }

    public List<CaseGroup> findAll(User caller) {
        return caseGroupRepository.findAll().stream()
                .filter(group -> hasReadAccess(group, caller))
                .toList();
    }

    public CaseGroup findOne(int id, User caller) {
        CaseGroup group = caseGroupRepository.findById(id)
                .orElseThrow(CaseGroupNotFoundException::new);
        if (!hasReadAccess(group, caller)) {
            throw new org.springframework.security.access.AccessDeniedException("No access to case group");
        }
        return group;
    }

    @Transactional
    public CaseGroup create(CreateGroupCommand cmd, User caller) {
        ensureWriteAllowed(caller);
        if (caseGroupRepository.existsByName(cmd.name())) {
            throw new CaseGroupAlreadyExistsException("Case group with name '" + cmd.name() + "' already exists");
        }
        if (caseGroupRepository.existsBySlug(cmd.slug())) {
            throw new CaseGroupAlreadyExistsException("Case group with slug '" + cmd.slug() + "' already exists");
        }

        CaseGroup group = new CaseGroup();
        group.setName(cmd.name());
        group.setSlug(cmd.slug());

        if (cmd.projectId() != null) {
            Project project = projectRepository.findByIdWithMembers(cmd.projectId())
                    .orElseThrow(() -> new ProjectNotFoundException("Couldn't find project with id: " + cmd.projectId()));
            ensureProjectWriteAccess(project, caller);
            group.setProject(project);
        } else if (!RoleSlugs.ADMIN.equals(caller.getRole().getSlug())) {
            throw new org.springframework.security.access.AccessDeniedException("Case group must be created inside a project");
        }

        return caseGroupRepository.save(group);
    }

    @Transactional
    public void update(int id, UpdateGroupCommand cmd, User caller) {
        CaseGroup group = findOne(id, caller);
        ensureWriteAllowed(caller);

        if (group.getProject() != null) {
            ensureProjectWriteAccess(group.getProject(), caller);
        }

        if (cmd.name() != null && !cmd.name().equals(group.getName())) {
            if (caseGroupRepository.existsByName(cmd.name())) {
                throw new CaseGroupAlreadyExistsException("Case group with name '" + cmd.name() + "' already exists");
            }
            group.setName(cmd.name());
        }

        if (cmd.slug() != null && !cmd.slug().equals(group.getSlug())) {
            if (caseGroupRepository.existsBySlug(cmd.slug())) {
                throw new CaseGroupAlreadyExistsException("Case group with slug '" + cmd.slug() + "' already exists");
            }
            group.setSlug(cmd.slug());
        }

        if (cmd.projectId() != null) {
            Project project = projectRepository.findByIdWithMembers(cmd.projectId())
                    .orElseThrow(() -> new ProjectNotFoundException("Couldn't find project with id: " + cmd.projectId()));
            ensureProjectWriteAccess(project, caller);
            group.setProject(project);
        }

        caseGroupRepository.save(group);
    }

    @Transactional
    public void delete(int id, User caller) {
        CaseGroup group = findOne(id, caller);
        ensureWriteAllowed(caller);
        if (group.getProject() != null) {
            ensureProjectWriteAccess(group.getProject(), caller);
        }
        if (caseRepository.existsByGroupId(id)) {
            throw new CaseGroupNotDeletableException(
                    "Case group with id '" + id + "' cannot be deleted because it still contains cases"
            );
        }
        caseGroupRepository.delete(group);
    }

    private boolean hasReadAccess(CaseGroup group, User caller) {
        if (caller == null) {
            return false;
        }
        String slug = caller.getRole().getSlug();
        if (RoleSlugs.ADMIN.equals(slug)) {
            return true;
        }
        if (group.getProject() == null) {
            return RoleSlugs.TEAMLEAD.equals(slug);
        }
        Integer projectId = group.getProject().getId();
        boolean isOwner = Objects.equals(group.getProject().getOwner().getId(), caller.getId());
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, caller.getId());
        if (RoleSlugs.TEAMLEAD.equals(slug)) {
            return isOwner || isMember;
        }
        return isMember;
    }

    private void ensureProjectWriteAccess(Project project, User caller) {
        if (caller == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        String slug = caller.getRole().getSlug();
        if (RoleSlugs.ADMIN.equals(slug)) return;
        boolean isOwner = Objects.equals(project.getOwner().getId(), caller.getId());
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), caller.getId());
        if (RoleSlugs.TEAMLEAD.equals(slug)) {
            if (!isOwner && !isMember) {
                throw new org.springframework.security.access.AccessDeniedException("Teamlead can only modify owned or entrusted project groups");
            }
            return;
        }
        if (RoleSlugs.USER.equals(slug)) {
            if (!isMember) {
                throw new org.springframework.security.access.AccessDeniedException("User can only modify entrusted project groups");
            }
            return;
        }
        throw new org.springframework.security.access.AccessDeniedException("No access to modify case groups");
    }

    private void ensureWriteAllowed(User caller) {
        if (caller == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        String slug = caller.getRole().getSlug();
        if (!RoleSlugs.ADMIN.equals(slug) && !RoleSlugs.TEAMLEAD.equals(slug) && !RoleSlugs.USER.equals(slug)) {
            throw new org.springframework.security.access.AccessDeniedException("No access to modify case groups");
        }
    }
}
