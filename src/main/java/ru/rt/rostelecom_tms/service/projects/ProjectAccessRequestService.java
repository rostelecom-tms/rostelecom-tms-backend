package ru.rt.rostelecom_tms.service.projects;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.config.cache.CacheNames;
import ru.rt.rostelecom_tms.domain.projects.Project;
import ru.rt.rostelecom_tms.domain.projects.ProjectAccessRequest;
import ru.rt.rostelecom_tms.domain.projects.ProjectAccessRequestDestination;
import ru.rt.rostelecom_tms.domain.projects.ProjectAccessRequestStatus;
import ru.rt.rostelecom_tms.domain.projects.ProjectMember;
import ru.rt.rostelecom_tms.domain.projects.exceptions.ProjectAccessDeniedException;
import ru.rt.rostelecom_tms.domain.projects.exceptions.ProjectAccessRequestAlreadyExistsException;
import ru.rt.rostelecom_tms.domain.projects.exceptions.ProjectAccessRequestNotFoundException;
import ru.rt.rostelecom_tms.domain.projects.exceptions.ProjectNotFoundException;
import ru.rt.rostelecom_tms.domain.users.RoleSlugs;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.repository.projects.ProjectAccessRequestRepository;
import ru.rt.rostelecom_tms.repository.projects.ProjectMemberRepository;
import ru.rt.rostelecom_tms.repository.projects.ProjectRepository;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProjectAccessRequestService {

    private final ProjectAccessRequestRepository projectAccessRequestRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PROJECTS_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.PLANS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.CASES_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.RUNS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD, allEntries = true)
    })
    public ProjectAccessRequest create(Integer projectId, String comment, User caller) {
        if (caller == null) {
            throw new ProjectAccessDeniedException("Authentication required");
        }

        String slug = caller.getRole().getSlug();
        if (!RoleSlugs.TEAMLEAD.equals(slug) && !RoleSlugs.USER.equals(slug)) {
            throw new ProjectAccessDeniedException("Only teamlead and user can request project access");
        }

        Project project = projectRepository.findOneById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Couldn't find project with id: " + projectId));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, caller.getId())) {
            throw new ProjectAccessRequestAlreadyExistsException("User already has access to this project");
        }

        if (projectAccessRequestRepository.existsByProjectIdAndRequesterUserIdAndStatus(
                projectId,
                caller.getId(),
                ProjectAccessRequestStatus.PENDING
        )) {
            throw new ProjectAccessRequestAlreadyExistsException("Pending request already exists for this project");
        }

        ProjectAccessRequest request = new ProjectAccessRequest();
        request.setProject(project);
        request.setRequesterUser(caller);
        request.setComment(comment == null || comment.isBlank() ? null : comment.trim());
        request.setStatus(ProjectAccessRequestStatus.PENDING);
        request.setCreatedAt(Instant.now());

        if (RoleSlugs.TEAMLEAD.equals(slug)) {
            request.setDestination(ProjectAccessRequestDestination.ADMIN);
            request.setApproverUser(null);
        } else {
            User owner = project.getOwner();
            if (owner != null && owner.getRole() != null && RoleSlugs.TEAMLEAD.equals(owner.getRole().getSlug())) {
                request.setDestination(ProjectAccessRequestDestination.TEAMLEAD);
                request.setApproverUser(owner);
            } else {
                request.setDestination(ProjectAccessRequestDestination.ADMIN);
                request.setApproverUser(null);
            }
        }

        return projectAccessRequestRepository.save(request);
    }

    public List<ProjectAccessRequest> inbox(User caller) {
        if (caller == null) {
            throw new ProjectAccessDeniedException("Authentication required");
        }

        String slug = caller.getRole().getSlug();
        if (RoleSlugs.ADMIN.equals(slug)) {
            return projectAccessRequestRepository.findByDestinationAndStatusOrderByCreatedAtAsc(
                    ProjectAccessRequestDestination.ADMIN,
                    ProjectAccessRequestStatus.PENDING
            );
        }

        if (RoleSlugs.TEAMLEAD.equals(slug)) {
            return projectAccessRequestRepository.findByApproverUserIdAndStatusOrderByCreatedAtAsc(
                    caller.getId(),
                    ProjectAccessRequestStatus.PENDING
            );
        }

        throw new ProjectAccessDeniedException("Only admin and teamlead can review requests");
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PROJECTS_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.PLANS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.CASES_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.RUNS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD, allEntries = true)
    })
    public void approve(Integer requestId, String decisionComment, User caller) {
        ProjectAccessRequest request = findEditableRequest(requestId, caller);

        request.setStatus(ProjectAccessRequestStatus.APPROVED);
        request.setDecisionComment(decisionComment == null || decisionComment.isBlank() ? null : decisionComment.trim());
        request.setProcessedAt(Instant.now());
        request.setProcessedByUser(caller);

        Integer projectId = request.getProject().getId();
        Integer userId = request.getRequesterUser().getId();
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            ProjectMember member = new ProjectMember();
            member.setProject(request.getProject());
            member.setUser(request.getRequesterUser());
            member.setAddedAt(Instant.now());
            projectMemberRepository.save(member);
        }

        projectAccessRequestRepository.save(request);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PROJECTS_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.PLANS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.CASES_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.RUNS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD, allEntries = true)
    })
    public void reject(Integer requestId, String decisionComment, User caller) {
        ProjectAccessRequest request = findEditableRequest(requestId, caller);

        request.setStatus(ProjectAccessRequestStatus.REJECTED);
        request.setDecisionComment(decisionComment == null || decisionComment.isBlank() ? null : decisionComment.trim());
        request.setProcessedAt(Instant.now());
        request.setProcessedByUser(caller);

        projectAccessRequestRepository.save(request);
    }

    private ProjectAccessRequest findEditableRequest(Integer requestId, User caller) {
        if (caller == null) {
            throw new ProjectAccessDeniedException("Authentication required");
        }

        ProjectAccessRequest request = projectAccessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ProjectAccessRequestNotFoundException("Couldn't find request with id: " + requestId));

        if (request.getStatus() != ProjectAccessRequestStatus.PENDING) {
            throw new ProjectAccessDeniedException("Only pending requests can be processed");
        }

        String slug = caller.getRole().getSlug();
        if (RoleSlugs.ADMIN.equals(slug)) {
            if (request.getDestination() != ProjectAccessRequestDestination.ADMIN) {
                throw new ProjectAccessDeniedException("This request is not routed to admin");
            }
            return request;
        }

        if (RoleSlugs.TEAMLEAD.equals(slug)) {
            boolean routedToCaller = request.getDestination() == ProjectAccessRequestDestination.TEAMLEAD
                    && request.getApproverUser() != null
                    && request.getApproverUser().getId().equals(caller.getId());
            if (!routedToCaller) {
                throw new ProjectAccessDeniedException("This request is not assigned to current teamlead");
            }
            return request;
        }

        throw new ProjectAccessDeniedException("Current role cannot process requests");
    }
}
