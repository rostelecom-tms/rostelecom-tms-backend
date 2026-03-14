package ru.rt.rostelecom_tms.service.plans;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.plans.Plan;
import ru.rt.rostelecom_tms.domain.plans.PlansCase;
import ru.rt.rostelecom_tms.domain.plans.exceptions.PlansCaseAlreadyExistsException;
import ru.rt.rostelecom_tms.domain.plans.exceptions.PlansCaseNotFoundException;
import ru.rt.rostelecom_tms.repository.plans.PlansCaseRepository;
import ru.rt.rostelecom_tms.service.cases.CaseService;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlansCaseService {

    private final PlansCaseRepository plansCaseRepository;
    private final PlanService planService;
    private final CaseService caseService;

    public record CreatePlansCaseCommand(
            Integer planId,
            Integer caseId
    ) {}

    public List<PlansCase> findAllByPlan(int planId) {
        planService.findOne(planId);
        return plansCaseRepository.findAllByPlanIdWithCase(planId);
    }

    public PlansCase findOne(int planId, int id) {
        PlansCase pc = plansCaseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new PlansCaseNotFoundException("Couldn't find plans-case entry with id: " + id));
        if (!pc.getPlan().getId().equals(planId)) {
            throw new PlansCaseNotFoundException(
                    "Plans-case entry with id " + id + " does not belong to plan with id " + planId
            );
        }
        return pc;
    }

    @Transactional
    public PlansCase create(CreatePlansCaseCommand cmd) {
        Plan plan = planService.findOne(cmd.planId());
        Case testCase = caseService.findOne(cmd.caseId());

        PlansCase plansCase = new PlansCase();
        plansCase.setPlan(plan);
        plansCase.setCaseField(testCase);

        try {
            return plansCaseRepository.save(plansCase);
        } catch (DataIntegrityViolationException e) {
            throw new PlansCaseAlreadyExistsException(
                    "Case with id '" + cmd.caseId() + "' is already added to plan with id '" + cmd.planId() + "'"
            );
        }
    }

    @Transactional
    public void delete(int planId, int id) {
        findOne(planId, id);
        plansCaseRepository.deleteById(id);
    }
}
