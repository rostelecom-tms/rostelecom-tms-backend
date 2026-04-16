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
import ru.rt.rostelecom_tms.repository.cases.CaseGroupRepository;
import ru.rt.rostelecom_tms.repository.cases.CaseRepository;
import ru.rt.rostelecom_tms.repository.projects.ProjectMemberRepository;
import ru.rt.rostelecom_tms.repository.projects.ProjectRepository;

import java.util.ArrayDeque;
import java.util.ArrayList;
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

    public record CreateGroupCommand(String name, String slug, Integer projectId, Integer parentId) {
    }

    public record UpdateGroupCommand(String name, String slug, Integer projectId, Integer parentId) {
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

    public List<Integer> findSubtreeGroupIds(int rootId, User caller) {
        CaseGroup root = findOne(rootId, caller);
        List<Integer> ids = new ArrayList<>();
        ArrayDeque<CaseGroup> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            CaseGroup current = queue.poll();
            ids.add(current.getId());
            queue.addAll(caseGroupRepository.findAllByParentId(current.getId()));
        }

        return ids;
    }

    @Transactional
    public CaseGroup create(CreateGroupCommand cmd, User caller) {
        ensureWriteAllowed(caller);
        ensureNameAndSlugAreUnique(cmd.name(), cmd.slug(), null);

        CaseGroup group = new CaseGroup();
        group.setName(cmd.name());
        group.setSlug(cmd.slug());

        CaseGroup parent = null;
        Project targetProject = null;

        if (cmd.parentId() != null) {
            parent = findWritableParent(cmd.parentId(), caller);
            targetProject = parent.getProject();

            if (cmd.projectId() != null) {
                Project explicitProject = findWritableProject(cmd.projectId(), caller);
                if (!sameProject(targetProject, explicitProject)) {
                    throw new IllegalArgumentException("Nested group must belong to the same project as its parent");
                }
            }

            group.setParent(parent);
            group.setProject(targetProject);
        } else if (cmd.projectId() != null) {
            targetProject = findWritableProject(cmd.projectId(), caller);
            group.setProject(targetProject);
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
            ensureNameAndSlugAreUnique(cmd.name(), null, group);
            group.setName(cmd.name());
        }

        if (cmd.slug() != null && !cmd.slug().equals(group.getSlug())) {
            ensureNameAndSlugAreUnique(null, cmd.slug(), group);
            group.setSlug(cmd.slug());
        }

        CaseGroup targetParent = group.getParent();
        if (cmd.parentId() != null) {
            targetParent = findWritableParent(cmd.parentId(), caller);
            ensureNoCycle(group, targetParent);
            group.setParent(targetParent);
        }

        Project targetProject = group.getProject();
        if (cmd.projectId() != null) {
            targetProject = findWritableProject(cmd.projectId(), caller);
        }

        if (targetParent != null) {
            Project parentProject = targetParent.getProject();
            if (targetProject == null) {
                targetProject = parentProject;
            }
            if (!sameProject(targetProject, parentProject)) {
                throw new IllegalArgumentException("Child group must belong to the same project as its parent");
            }
        }

        group.setProject(targetProject);
        caseGroupRepository.save(group);

        propagateProjectToDescendants(group, targetProject);
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
        if (caseGroupRepository.existsByParentId(id)) {
            throw new CaseGroupNotDeletableException(
                    "Case group with id '" + id + "' cannot be deleted because it still contains child groups"
            );
        }
        caseGroupRepository.delete(group);
    }

    private void propagateProjectToDescendants(CaseGroup parent, Project project) {
        List<CaseGroup> children = caseGroupRepository.findAllByParentId(parent.getId());
        for (CaseGroup child : children) {
            child.setProject(project);
            caseGroupRepository.save(child);
            propagateProjectToDescendants(child, project);
        }
    }

    private void ensureNoCycle(CaseGroup group, CaseGroup parent) {
        if (Objects.equals(group.getId(), parent.getId())) {
            throw new IllegalArgumentException("Group cannot be nested under itself");
        }

        CaseGroup cursor = parent;
        while (cursor != null) {
            if (Objects.equals(cursor.getId(), group.getId())) {
                throw new IllegalArgumentException("Group hierarchy cycle detected");
            }
            cursor = cursor.getParent();
        }
    }

    private void ensureNameAndSlugAreUnique(String name, String slug, CaseGroup current) {
        if (name != null && caseGroupRepository.existsByName(name) && (current == null || !name.equals(current.getName()))) {
            throw new CaseGroupAlreadyExistsException("Case group with name '" + name + "' already exists");
        }
        if (slug != null && caseGroupRepository.existsBySlug(slug) && (current == null || !slug.equals(current.getSlug()))) {
            throw new CaseGroupAlreadyExistsException("Case group with slug '" + slug + "' already exists");
        }
    }

    private CaseGroup findWritableParent(Integer parentId, User caller) {
        CaseGroup parent = caseGroupRepository.findById(parentId)
                .orElseThrow(CaseGroupNotFoundException::new);

        if (parent.getProject() != null) {
            ensureProjectWriteAccess(parent.getProject(), caller);
        } else {
            String slug = caller.getRole().getSlug();
            if (!RoleSlugs.ADMIN.equals(slug) && !RoleSlugs.TEAMLEAD.equals(slug)) {
                throw new org.springframework.security.access.AccessDeniedException("User can only modify case groups inside entrusted projects");
            }
        }

        return parent;
    }

    private Project findWritableProject(Integer projectId, User caller) {
        Project project = projectRepository.findOneById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Couldn't find project with id: " + projectId));
        ensureProjectWriteAccess(project, caller);
        return project;
    }

    private boolean sameProject(Project left, Project right) {
        Integer leftId = left == null ? null : left.getId();
        Integer rightId = right == null ? null : right.getId();
        return Objects.equals(leftId, rightId);
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
