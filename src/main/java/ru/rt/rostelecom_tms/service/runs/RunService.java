package ru.rt.rostelecom_tms.service.runs;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.config.cache.CacheNames;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseNotFoundException;
import ru.rt.rostelecom_tms.domain.plans.Plan;
import ru.rt.rostelecom_tms.domain.plans.exceptions.PlanNotFoundException;
import ru.rt.rostelecom_tms.domain.runs.Run;
import ru.rt.rostelecom_tms.domain.runs.RunStatus;
import ru.rt.rostelecom_tms.domain.runs.exceptions.RunStatusNotFoundException;
import ru.rt.rostelecom_tms.domain.users.RoleSlugs;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.domain.users.exceptions.UserNotFoundException;
import ru.rt.rostelecom_tms.repository.projects.ProjectMemberRepository;
import ru.rt.rostelecom_tms.repository.cases.CaseRepository;
import ru.rt.rostelecom_tms.repository.plans.PlanRepository;
import ru.rt.rostelecom_tms.repository.runs.RunRepository;
import ru.rt.rostelecom_tms.repository.runs.RunStatusRepository;
import ru.rt.rostelecom_tms.repository.users.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RunService {

    private final RunRepository runRepository;
    private final RunStatusRepository runStatusRepository;
    private final PlanRepository planRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public record CreateRunCommand(
        Integer planId,
        Integer caseId,
        Integer statusId,
        Integer executedBy,
        Instant executedAt
    ) {}

    public record CreateRunCommandFromBulk(
            Integer planId,
            Integer caseId,
            String statusSlug,
            Integer executedBy,
            Instant executedAt
    ) {}

    public record RunStatusView(
            Integer id,
            String name,
            String slug
    ) {}

    public List<Run> findAllWithProbableFilters(
            Integer planId,
            Integer caseId,
            Integer statusId,
            String statusSlug,
            Integer executedBy,
            Instant executedFrom,
            Instant executedTo,
            Integer groupId,
            User caller
    ) {
        return runRepository.findAllByOrderByExecutedAtDesc().stream()
                .filter(run -> planId == null || Objects.equals(run.getPlan().getId(), planId))
                .filter(run -> caseId == null || Objects.equals(run.getCaseField().getId(), caseId))
                .filter(run -> statusId == null || Objects.equals(run.getStatus().getId(), statusId))
                .filter(run -> statusSlug == null || statusSlug.isBlank() || statusSlug.equals(run.getStatus().getSlug()))
                .filter(run -> executedBy == null || (run.getExecutedBy() != null && Objects.equals(run.getExecutedBy().getId(), executedBy)))
                .filter(run -> groupId == null || Objects.equals(run.getCaseField().getGroup().getId(), groupId))
                .filter(run -> executedFrom == null || !run.getExecutedAt().isBefore(executedFrom))
                .filter(run -> executedTo == null || !run.getExecutedAt().isAfter(executedTo))
                .filter(run -> hasProjectReadAccess(run.getPlan(), caller))
                .toList();
    }

    public List<RunStatusView> listStatuses() {
        return runStatusRepository.findAll().stream()
                .map(status -> new RunStatusView(status.getId(), status.getName(), status.getSlug()))
                .toList();
    }



    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.RUNS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD, allEntries = true)
    })
    public Run createRun(CreateRunCommand cmd, User caller) {
        Plan plan = planRepository.findById(cmd.planId()).orElseThrow(() -> new PlanNotFoundException("Couldn't find plan with id: " + cmd.planId()));
        ensureProjectWriteAccess(plan, caller);

        Case caseFromRun = caseRepository.findOneById(cmd.caseId()).orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + cmd.caseId()));
        ensureCaseBelongsToPlan(caseFromRun, plan);

        User user = resolveExecutor(cmd.executedBy(), caller);

        RunStatus runStatus = runStatusRepository.findById(cmd.statusId()).orElseThrow(() -> new RunStatusNotFoundException("Couldn't find run status with id: " + cmd.statusId()));
        return runRepository.save(buildRun(plan, caseFromRun, runStatus, user, cmd.executedAt()));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.RUNS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD, allEntries = true)
    })
    public List<Run> createRunsBulk(List<CreateRunCommandFromBulk> commands, User caller) {
        List<Run> runs = commands.stream().map(
                cmd -> {
                    Case caseFromRun = caseRepository.findOneById(cmd.caseId()).orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + cmd.caseId()));

                    Plan plan = planRepository.findById(cmd.planId()).orElseThrow(() -> new PlanNotFoundException("Couldn't find plan with id: " + cmd.planId()));
                    ensureProjectWriteAccess(plan, caller);
                    ensureCaseBelongsToPlan(caseFromRun, plan);

                    User user = resolveExecutor(cmd.executedBy(), caller);

                    RunStatus runStatus = runStatusRepository.findBySlug(cmd.statusSlug()).orElseThrow(() -> new RunStatusNotFoundException("Couldn't find run status with slug: " + cmd.statusSlug()));
                    return buildRun(plan, caseFromRun, runStatus, user, cmd.executedAt());
                }
                ).toList();

        return runRepository.saveAll(runs);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.RUNS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD, allEntries = true)
    })
    public List<Run> createRunsBulkFromIntegration(List<CreateRunCommandFromBulk> commands) {
        List<Run> runs = commands.stream()
                .map(cmd -> {
                    Case caseFromRun = caseRepository.findOneById(cmd.caseId()).orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + cmd.caseId()));
                    Plan plan = planRepository.findById(cmd.planId()).orElseThrow(() -> new PlanNotFoundException("Couldn't find plan with id: " + cmd.planId()));
                    ensureCaseBelongsToPlan(caseFromRun, plan);

                    User user = resolveExecutor(cmd.executedBy(), null);
                    RunStatus runStatus = runStatusRepository.findBySlug(cmd.statusSlug()).orElseThrow(() -> new RunStatusNotFoundException("Couldn't find run status with slug: " + cmd.statusSlug()));

                    return buildRun(plan, caseFromRun, runStatus, user, cmd.executedAt());
                })
                .toList();

        return runRepository.saveAll(runs);
    }

    private Run buildRun(Plan plan, Case caseFromRun, RunStatus runStatus, User executedBy, Instant executedAt) {
        Run run = new Run();
        run.setPlan(plan);
        run.setCaseField(caseFromRun);
        run.setStatus(runStatus);
        run.setExecutedBy(executedBy);
        run.setExecutedAt(executedAt == null ? Instant.now() : executedAt);
        return run;
    }

    private User resolveExecutor(Integer executedById, User fallbackCaller) {
        if (executedById != null) {
            return userRepository.findById(executedById)
                    .orElseThrow(() -> new UserNotFoundException("Couldn't find user with id: " + executedById));
        }

        return fallbackCaller;
    }

    private void ensureCaseBelongsToPlan(Case caseFromRun, Plan plan) {
        boolean belongsToPlan = plan.getCases().stream()
                .anyMatch(planCase -> Objects.equals(planCase.getId(), caseFromRun.getId()));

        if (!belongsToPlan) {
            throw new IllegalArgumentException("Case with id '" + caseFromRun.getId() + "' is not attached to plan with id '" + plan.getId() + "'");
        }
    }

    private boolean hasProjectReadAccess(Plan plan, User caller) {
        if (caller == null) {
            return false;
        }

        String role = caller.getRole().getSlug();
        if (RoleSlugs.ADMIN.equals(role)) {
            return true;
        }

        if (plan.getProject() == null) {
            if (!RoleSlugs.TEAMLEAD.equals(role)) {
                return false;
            }
            User responsible = plan.getResponsibleUser();
            return responsible != null && Objects.equals(responsible.getId(), caller.getId());
        }

        Integer projectId = plan.getProject().getId();
        boolean isOwner = Objects.equals(plan.getProject().getOwner().getId(), caller.getId());
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, caller.getId());

        if (RoleSlugs.TEAMLEAD.equals(role)) {
            return isOwner || isMember;
        }

        return isMember;
    }

    private void ensureProjectWriteAccess(Plan plan, User caller) {
        if (!hasProjectReadAccess(plan, caller)) {
            throw new org.springframework.security.access.AccessDeniedException("No access to project's runs");
        }
    }
}
