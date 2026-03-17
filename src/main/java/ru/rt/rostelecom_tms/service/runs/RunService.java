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
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.domain.users.exceptions.UserNotFoundException;
import ru.rt.rostelecom_tms.repository.cases.CaseRepository;
import ru.rt.rostelecom_tms.repository.plans.PlanRepository;
import ru.rt.rostelecom_tms.repository.runs.RunRepository;
import ru.rt.rostelecom_tms.repository.runs.RunStatusRepository;
import ru.rt.rostelecom_tms.repository.users.UserRepository;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RunService {

    private final RunRepository runRepository;
    private final RunStatusRepository runStatusRepository;
    private final PlanRepository planRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;

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
            Integer groupId
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

        return runsFromServiceResponse;
    }



    @Transactional
    public Run createRun(CreateRunCommand cmd) {
        Plan plan = planRepository.findById(cmd.planId()).orElseThrow(() -> new PlanNotFoundException("Couldn't find plan with id: " + cmd.planId()));

        Case caseFromRun = caseRepository.findByIdWithSteps(cmd.caseId()).orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + cmd.caseId()));

        User user = userRepository.findById(cmd.executedBy()).orElseThrow(() -> new UserNotFoundException("Couldn't find user with id: " + cmd.executedBy));

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
    public List<Run> createRunsBulk(List<CreateRunCommandFromBulk> commands) {

        List<Run> runs = commands.stream().map(
                cmd -> {
                    Case caseFromRun = caseRepository.findByIdWithSteps(cmd.caseId()).orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + cmd.caseId()));

                    Plan plan = planRepository.findById(cmd.planId()).orElseThrow(() -> new PlanNotFoundException("Couldn't find plan with id: " + cmd.planId()));

                    User user = userRepository.findById(cmd.executedBy()).orElseThrow(() -> new UserNotFoundException("Couldn't find user with id: " + cmd.executedBy));

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
}
