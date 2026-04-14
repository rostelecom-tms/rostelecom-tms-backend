package ru.rt.rostelecom_tms.service.runs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        List<Run> runsFromServiceResponse;

        if (planId != null) {
            runsFromServiceResponse = runRepository.findByPlanId(planId);
        } else if (caseId != null) {
            runsFromServiceResponse = runRepository.findByCaseFieldId(caseId);
        } else if (statusId != null) {
            runsFromServiceResponse = runRepository.findByStatusId(statusId);
        } else if (statusSlug != null) {
            runsFromServiceResponse = runRepository.findByStatusSlug(statusSlug);
        } else if (executedBy != null) {
            runsFromServiceResponse = runRepository.findByExecutedById(executedBy);
        } else if (executedFrom != null && executedTo != null) {
            runsFromServiceResponse = runRepository.findByExecutedAtBetween(executedFrom, executedTo);
        } else if (groupId != null) {
            runsFromServiceResponse = runRepository.findByCaseFieldGroupId(groupId);
        } else {
            runsFromServiceResponse = runRepository.findAll();
        }

        return runsFromServiceResponse.stream()
                .filter(run -> hasProjectReadAccess(run.getPlan(), caller))
                .toList();
    }



    @Transactional
    public Run createRun(CreateRunCommand cmd, User caller) {
        Plan plan = planRepository.findById(cmd.planId()).orElseThrow(() -> new PlanNotFoundException("Couldn't find plan with id: " + cmd.planId()));
        ensureProjectWriteAccess(plan, caller);

        Case caseFromRun = caseRepository.findOneById(cmd.caseId()).orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + cmd.caseId()));

        User user = userRepository.findById(cmd.executedBy()).orElseThrow(() -> new UserNotFoundException("Couldn't find user with id: " + cmd.executedBy()));

        RunStatus runStatus = runStatusRepository.findById(cmd.statusId()).orElseThrow(() -> new RunStatusNotFoundException("Couldn't find run status with id: " + cmd.statusId()));

        Run run = new Run();

        run.setPlan(plan);
        run.setCaseField(caseFromRun);
        run.setStatus(runStatus);
        run.setExecutedBy(user);
        run.setExecutedAt(cmd.executedAt());

        return runRepository.save(run);
    }

    @Transactional
    public List<Run> createRunsBulk(List<CreateRunCommandFromBulk> commands, User caller) {

        List<Run> runs = commands.stream().map(
                cmd -> {
                    Case caseFromRun = caseRepository.findOneById(cmd.caseId()).orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + cmd.caseId()));

                    Plan plan = planRepository.findById(cmd.planId()).orElseThrow(() -> new PlanNotFoundException("Couldn't find plan with id: " + cmd.planId()));
                    ensureProjectWriteAccess(plan, caller);

                    User user = userRepository.findById(cmd.executedBy()).orElseThrow(() -> new UserNotFoundException("Couldn't find user with id: " + cmd.executedBy()));

                    RunStatus runStatus = runStatusRepository.findBySlug(cmd.statusSlug()).orElseThrow(() -> new RunStatusNotFoundException("Couldn't find run status with slug: " + cmd.statusSlug()));

                    Run run = new Run();

                    run.setPlan(plan);
                    run.setCaseField(caseFromRun);
                    run.setStatus(runStatus);
                    run.setExecutedBy(user);
                    run.setExecutedAt(cmd.executedAt());

                    return run;
                }
                ).toList();

        return runs.stream().map(runRepository::save).toList();
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
