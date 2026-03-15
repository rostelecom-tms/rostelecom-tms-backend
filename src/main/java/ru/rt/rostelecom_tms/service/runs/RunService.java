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
import java.util.Set;

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

    public List<Run> findAll() { return runRepository.findAllWithPlanAndCase(); }

    public List<Run> findAllByPlanId(Integer planId) { return runRepository.findRunsByPlanId(planId); }

    public List<Run> findAllByCaseId(Integer caseId) { return runRepository.findRunsByCaseId(caseId); }

    public List<Run> findAllByStatusId(Integer statusId) { return runRepository.findRunsByStatusId(statusId); }

    public List<Run> findAllByStatusSlug(String slug) { return runRepository.findRunsByStatusSlug(slug); }

    public List<Run> findAllByExecutedBy(Integer executedBy) { return runRepository.findRunsByExecutedBy(executedBy); }

    public List<Run> findAllByExecutedFromAndTo(Instant executedFrom, Instant executedTo) { return runRepository.findRunsByExecutedFromAndTo(executedFrom, executedTo); }

    public List<Run> findAllByGroupId(Integer groupId) { return runRepository.findRunsByCaseFieldGroupId(groupId); }

    public Run createRun(CreateRunCommand cmd) {
        Plan plan = planRepository.findById(cmd.planId()).orElseThrow(() -> new PlanNotFoundException("Couldn't find plan with id: " + cmd.planId()));

        Case caseFromRun = caseRepository.findByIdWithSteps(cmd.caseId).orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + cmd.caseId()));

        User user = userRepository.findById(cmd.executedBy).orElseThrow(() -> new UserNotFoundException("Couldn't find user with id: " + cmd.executedBy));

        RunStatus runStatus = runStatusRepository.findById(cmd.statusId()).orElseThrow(() -> new RunStatusNotFoundException("Couldn't find run with id: " + cmd.statusId()));

        Run run = new Run();

        run.setPlan(plan);
        run.setCaseField(caseFromRun);
        run.setStatus(runStatus);
        run.setExecutedBy(user);
        run.setExecutedAt(cmd.executedAt());

        Set<Run> allRuns = plan.getRuns();
        allRuns.add(run);
        plan.setRuns(allRuns);

        allRuns = caseFromRun.getRuns();
        allRuns.add(run);
        caseFromRun.setRuns(allRuns);

        allRuns = user.getRuns();
        allRuns.add(run);
        user.setRuns(allRuns);

        planRepository.save(plan);
        caseRepository.save(caseFromRun);
        userRepository.save(user);

        return runRepository.save(run);
    }

    public Run createRun(CreateRunCommandFromBulk cmd) {
        Integer statusId = runStatusRepository.findBySlug(cmd.statusSlug()).orElseThrow(() -> new RunStatusNotFoundException("Couldn't find run with slug: " + cmd.statusSlug())).getId();

        return createRun(
                new CreateRunCommand(
                    cmd.planId(),
                    cmd.caseId(),
                    statusId,
                    cmd.executedBy(),
                    cmd.executedAt()
                )
        );
    }
}
