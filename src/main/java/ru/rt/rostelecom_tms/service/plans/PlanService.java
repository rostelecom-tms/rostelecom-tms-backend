package ru.rt.rostelecom_tms.service.plans;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseAlreadyInPlanException;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseNotFoundException;
import ru.rt.rostelecom_tms.domain.plans.Plan;
import ru.rt.rostelecom_tms.domain.plans.exceptions.PlanAccessDeniedException;
import ru.rt.rostelecom_tms.domain.plans.exceptions.PlanAlreadyExistsException;
import ru.rt.rostelecom_tms.domain.plans.exceptions.PlanCreationNotAllowedException;
import ru.rt.rostelecom_tms.domain.plans.exceptions.PlanNotFoundException;
import ru.rt.rostelecom_tms.domain.projects.Project;
import ru.rt.rostelecom_tms.domain.projects.exceptions.ProjectNotFoundException;
import ru.rt.rostelecom_tms.domain.users.RoleSlugs;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.repository.projects.ProjectMemberRepository;
import ru.rt.rostelecom_tms.repository.projects.ProjectRepository;
import ru.rt.rostelecom_tms.repository.plans.PlanRepository;
import ru.rt.rostelecom_tms.service.cases.CaseService;
import ru.rt.rostelecom_tms.service.users.UserService;

import java.time.Instant;
import java.util.HashSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final UserService userService;
    private final CaseService caseService;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public record CreatePlanCommand(
            String name,
            String introduction,
            String approach,
            LocalDate startDate,
            LocalDate endDate,
            Integer responsibleUserId,
            Integer projectId
    ) {}

    public record UpdatePlanCommand(
            String name,
            String introduction,
            String approach,
            LocalDate startDate,
            LocalDate endDate,
            Integer responsibleUserId,
            Integer projectId
    ) {}

    public List<Plan> findAll(User caller) {
        if (caller == null) {
            return List.of();
        }

        List<Plan> plans = planRepository.findAllBy();
        if (plans.isEmpty()) {
            return plans;
        }

        String slug = caller.getRole().getSlug();
        List<Plan> visible;

        if (RoleSlugs.ADMIN.equals(slug)) {
            visible = plans;
        } else {
            Set<Integer> accessibleProjectIds = RoleSlugs.TEAMLEAD.equals(slug)
                    ? projectRepository.findDistinctByOwnerIdOrMembersUserId(caller.getId(), caller.getId()).stream().map(Project::getId).collect(java.util.stream.Collectors.toSet())
                    : new HashSet<>(projectRepository.findDistinctByMembersUserId(caller.getId()).stream().map(Project::getId).toList());

            visible = plans.stream()
                    .filter(plan -> {
                        if (plan.getProject() != null) {
                            return accessibleProjectIds.contains(plan.getProject().getId());
                        }
                        if (!RoleSlugs.TEAMLEAD.equals(slug)) {
                            return false;
                        }
                        User responsible = plan.getResponsibleUser();
                        return responsible != null && Objects.equals(responsible.getId(), caller.getId());
                    })
                    .toList();
        }

        if (visible.isEmpty()) return visible;
        return planRepository.findDistinctByIdIn(visible.stream().map(Plan::getId).toList());
    }

    public List<Plan> findAllWithFilters(
            String name,
            Integer responsibleUserId,
            Integer projectId,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo,
            User caller
    ) {
        String normalizedName = name == null ? null : name.trim().toLowerCase();

        return findAll(caller).stream()
                .filter(plan -> normalizedName == null || normalizedName.isBlank()
                        || plan.getName().toLowerCase().contains(normalizedName))
                .filter(plan -> responsibleUserId == null
                        || (plan.getResponsibleUser() != null && Objects.equals(plan.getResponsibleUser().getId(), responsibleUserId)))
                .filter(plan -> projectId == null
                        || (plan.getProject() != null && Objects.equals(plan.getProject().getId(), projectId)))
                .filter(plan -> startDateFrom == null
                        || (plan.getStartDate() != null && !plan.getStartDate().isBefore(startDateFrom)))
                .filter(plan -> startDateTo == null
                        || (plan.getStartDate() != null && !plan.getStartDate().isAfter(startDateTo)))
                .filter(plan -> endDateFrom == null
                        || (plan.getEndDate() != null && !plan.getEndDate().isBefore(endDateFrom)))
                .filter(plan -> endDateTo == null
                        || (plan.getEndDate() != null && !plan.getEndDate().isAfter(endDateTo)))
                .toList();
    }

    private Plan findOne(int id) {
        return planRepository.findOneById(id)
                .orElseThrow(() -> new PlanNotFoundException("Couldn't find plan with id: " + id));
    }

    public Plan findOne(int id, User caller) {
        Plan plan = findOne(id);
        checkReadAccess(plan, caller);
        return plan;
    }

    @Transactional
    public Plan create(CreatePlanCommand cmd, User caller) {
        checkCanCreate(caller);
        ensurePlanNameIsUnique(cmd.name(), null);
        validateDates(cmd.startDate(), cmd.endDate());

        Plan plan = new Plan();
        plan.setName(cmd.name());
        plan.setIntroduction(cmd.introduction());
        plan.setApproach(cmd.approach());
        plan.setStartDate(cmd.startDate());
        plan.setEndDate(cmd.endDate());
        plan.setCreatedAt(Instant.now());

        if (cmd.projectId() != null) {
            Project project = projectRepository.findOneById(cmd.projectId())
                    .orElseThrow(() -> new ProjectNotFoundException("Couldn't find project with id: " + cmd.projectId()));
            ensureProjectWriteAccess(project, caller);
            plan.setProject(project);
        } else if (!RoleSlugs.ADMIN.equals(caller.getRole().getSlug())) {
            throw new PlanCreationNotAllowedException("Plan must be created inside a project");
        }

        if (RoleSlugs.TEAMLEAD.equals(caller.getRole().getSlug())) {
            plan.setResponsibleUser(caller);
        } else if (cmd.responsibleUserId() != null) {
            User user = userService.findOne(cmd.responsibleUserId());
            plan.setResponsibleUser(user);
        }

        Plan saved = planRepository.save(plan);
        return planRepository.findOneById(saved.getId())
                .orElseThrow(() -> new PlanNotFoundException("Couldn't reload plan after create, id: " + saved.getId()));
    }

    @Transactional
    public void update(int id, UpdatePlanCommand cmd, User caller) {
        Plan plan = findOne(id);
        checkWriteAccess(plan, caller);

        String nextName = cmd.name() != null ? cmd.name() : plan.getName();
        ensurePlanNameIsUnique(nextName, plan);

        LocalDate nextStart = cmd.startDate() != null ? cmd.startDate() : plan.getStartDate();
        LocalDate nextEnd   = cmd.endDate()   != null ? cmd.endDate()   : plan.getEndDate();
        validateDates(nextStart, nextEnd);

        if (cmd.name() != null) plan.setName(cmd.name());
        if (cmd.introduction() != null) plan.setIntroduction(cmd.introduction());
        if (cmd.approach() != null) plan.setApproach(cmd.approach());
        if (cmd.startDate() != null) plan.setStartDate(cmd.startDate());
        if (cmd.endDate() != null) plan.setEndDate(cmd.endDate());

        if (cmd.projectId() != null) {
            Project project = projectRepository.findOneById(cmd.projectId())
                .orElseThrow(() -> new ProjectNotFoundException("Couldn't find project with id: " + cmd.projectId()));
            ensureProjectWriteAccess(project, caller);
            plan.setProject(project);
        }

        if (!RoleSlugs.TEAMLEAD.equals(caller.getRole().getSlug())) {
            if (cmd.responsibleUserId() != null) {
                User user = userService.findOne(cmd.responsibleUserId());
                plan.setResponsibleUser(user);
            }
        }

        planRepository.save(plan);
    }

    @Transactional
    public void addCase(int planId, int caseId, User caller) {
        Plan plan = findOne(planId);
        checkWriteAccess(plan, caller);
        Case testCase = caseService.findOne(caseId, caller);

        boolean alreadyLinked = plan.getCases().stream()
                .anyMatch(c -> c.getId().equals(caseId));
        if (alreadyLinked) {
            throw new CaseAlreadyInPlanException(
                    "Case with id '" + caseId + "' is already added to plan with id '" + planId + "'"
            );
        }

        plan.getCases().add(testCase);
        planRepository.save(plan);
    }

    @Transactional
    public void removeCase(int planId, int caseId, User caller) {
        Plan plan = findOne(planId);
        checkWriteAccess(plan, caller);

        boolean removed = plan.getCases().removeIf(c -> c.getId().equals(caseId));
        if (!removed) {
            throw new CaseNotFoundException(
                    "Case with id '" + caseId + "' is not in plan with id '" + planId + "'"
            );
        }

        planRepository.save(plan);
    }

    @Transactional
    public void delete(int id, User caller) {
        Plan plan = findOne(id);
        checkWriteAccess(plan, caller);
        planRepository.deleteById(id);
    }

    private void checkCanCreate(User caller) {
        if (caller == null) {
            throw new PlanCreationNotAllowedException("Authentication required");
        }
        String slug = caller.getRole().getSlug();
        if (RoleSlugs.ADMIN.equals(slug)) return;
        if (RoleSlugs.TEAMLEAD.equals(slug) && !caller.isCanCreatePlans()) {
            throw new PlanCreationNotAllowedException(
                    "Teamlead does not have permission to create plans"
            );
        }
        if (!RoleSlugs.TEAMLEAD.equals(slug) && !RoleSlugs.USER.equals(slug)) {
            throw new PlanCreationNotAllowedException("Unsupported role for plan creation");
        }
    }

    private void checkReadAccess(Plan plan, User caller) {
        if (!hasReadAccess(plan, caller)) {
            throw new PlanAccessDeniedException("No read access to plan");
        }
    }

    private void checkWriteAccess(Plan plan, User caller) {
        if (caller == null) {
            throw new PlanAccessDeniedException("Authentication required");
        }
        String slug = caller.getRole().getSlug();
        if (RoleSlugs.ADMIN.equals(slug)) return;

        if (plan.getProject() != null) {
            Integer projectId = plan.getProject().getId();
            boolean isOwner = Objects.equals(plan.getProject().getOwner().getId(), caller.getId());
            boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, caller.getId());

            if (RoleSlugs.TEAMLEAD.equals(slug)) {
                if (isOwner || isMember) {
                    return;
                }
                throw new PlanAccessDeniedException("Teamlead can only modify owned or entrusted project plans");
            }
            if (RoleSlugs.USER.equals(slug)) {
                if (isMember) {
                    return;
                }
                throw new PlanAccessDeniedException("User can only modify plans in entrusted projects");
            }
            throw new PlanAccessDeniedException("No write access to plans");
        }

        if (RoleSlugs.TEAMLEAD.equals(slug)) {
            User responsible = plan.getResponsibleUser();
            if (responsible == null || !responsible.getId().equals(caller.getId())) {
                throw new PlanAccessDeniedException(
                        "Teamlead can only modify their own legacy plans"
                );
            }
            return;
        }
        throw new PlanAccessDeniedException("User cannot modify legacy plans outside projects");
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
    }

    private void ensurePlanNameIsUnique(String name, Plan currentPlan) {
        boolean duplicateExists = planRepository.existsByName(name);
        if (!duplicateExists) return;
        if (currentPlan != null && name.equals(currentPlan.getName())) return;
        throw new PlanAlreadyExistsException("Plan with name '" + name + "' already exists");
    }

    private boolean hasReadAccess(Plan plan, User caller) {
        if (caller == null) {
            return false;
        }
        String slug = caller.getRole().getSlug();
        if (RoleSlugs.ADMIN.equals(slug)) {
            return true;
        }

        if (plan.getProject() != null) {
            Integer projectId = plan.getProject().getId();
            boolean isOwner = Objects.equals(plan.getProject().getOwner().getId(), caller.getId());
            boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, caller.getId());
            if (RoleSlugs.TEAMLEAD.equals(slug)) {
                return isOwner || isMember;
            }
            return isMember;
        }

        if (RoleSlugs.TEAMLEAD.equals(slug)) {
            User responsible = plan.getResponsibleUser();
            return responsible != null && Objects.equals(responsible.getId(), caller.getId());
        }

        return false;
    }

    private void ensureProjectWriteAccess(Project project, User caller) {
        if (caller == null) {
            throw new PlanAccessDeniedException("Authentication required");
        }
        String slug = caller.getRole().getSlug();
        if (RoleSlugs.ADMIN.equals(slug)) {
            return;
        }
        boolean isOwner = Objects.equals(project.getOwner().getId(), caller.getId());
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), caller.getId());
        if (RoleSlugs.TEAMLEAD.equals(slug)) {
            if (!isOwner && !isMember) {
                throw new PlanAccessDeniedException("Teamlead can only manage owned or entrusted projects");
            }
            return;
        }
        if (RoleSlugs.USER.equals(slug)) {
            if (!isMember) {
                throw new PlanAccessDeniedException("User can only manage entrusted projects");
            }
            return;
        }
        throw new PlanAccessDeniedException("No access to manage project plans");
    }
}
