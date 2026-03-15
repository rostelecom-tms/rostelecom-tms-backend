package ru.rt.rostelecom_tms.service.plans;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseNotFoundException;
import ru.rt.rostelecom_tms.domain.plans.Plan;
import ru.rt.rostelecom_tms.domain.plans.exceptions.PlanAlreadyExistsException;
import ru.rt.rostelecom_tms.domain.plans.exceptions.PlanNotFoundException;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.repository.plans.PlanRepository;
import ru.rt.rostelecom_tms.service.cases.CaseService;
import ru.rt.rostelecom_tms.service.users.UserService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final UserService userService;
    private final CaseService caseService;

    public record CreatePlanCommand(
            String name,
            String introduction,
            String approach,
            LocalDate startDate,
            LocalDate endDate,
            Integer responsibleUserId
    ) {}

    public record UpdatePlanCommand(
            String name,
            String introduction,
            String approach,
            LocalDate startDate,
            LocalDate endDate,
            Integer responsibleUserId
    ) {}

    public List<Plan> findAll() {
        List<Plan> plans = planRepository.findAllWithUser();
        if (plans.isEmpty()) {
            return plans;
        }
        return planRepository.fetchCasesForPlans(plans);
    }

    public Plan findOne(int id) {
        return planRepository.findByIdWithCasesAndUser(id)
                .orElseThrow(() -> new PlanNotFoundException("Couldn't find plan with id: " + id));
    }

    @Transactional
    public Plan create(CreatePlanCommand cmd) {
        ensurePlanNameIsUnique(cmd.name(), null);
        validateDates(cmd.startDate(), cmd.endDate());

        Plan plan = new Plan();
        plan.setName(cmd.name());
        plan.setIntroduction(cmd.introduction());
        plan.setApproach(cmd.approach());
        plan.setStartDate(cmd.startDate());
        plan.setEndDate(cmd.endDate());
        plan.setCreatedAt(Instant.now());

        if (cmd.responsibleUserId() != null) {
            User user = userService.findOne(cmd.responsibleUserId());
            plan.setResponsibleUser(user);
        }

        Plan saved = planRepository.save(plan);
        return planRepository.findByIdWithCasesAndUser(saved.getId())
                .orElseThrow(() -> new PlanNotFoundException("Couldn't reload plan after create, id: " + saved.getId()));
    }

    @Transactional
    public void update(int id, UpdatePlanCommand cmd) {
        Plan plan = findOne(id);

        String nextName = cmd.name() != null ? cmd.name() : plan.getName();
        ensurePlanNameIsUnique(nextName, plan);

        LocalDate nextStart = cmd.startDate() != null ? cmd.startDate() : plan.getStartDate();
        LocalDate nextEnd   = cmd.endDate()   != null ? cmd.endDate()   : plan.getEndDate();
        validateDates(nextStart, nextEnd);

        if (cmd.name() != null) {
            plan.setName(cmd.name());
        }
        if (cmd.introduction() != null) {
            plan.setIntroduction(cmd.introduction());
        }
        if (cmd.approach() != null) {
            plan.setApproach(cmd.approach());
        }
        if (cmd.startDate() != null) {
            plan.setStartDate(cmd.startDate());
        }
        if (cmd.endDate() != null) {
            plan.setEndDate(cmd.endDate());
        }
        if (cmd.responsibleUserId() != null) {
            User user = userService.findOne(cmd.responsibleUserId());
            plan.setResponsibleUser(user);
        }

        planRepository.save(plan);
    }

    @Transactional
    public void addCase(int planId, int caseId) {
        Plan plan = findOne(planId);
        Case testCase = caseService.findOne(caseId);

        boolean alreadyLinked = plan.getCases().stream()
                .anyMatch(c -> c.getId().equals(caseId));
        if (alreadyLinked) {
            throw new PlanAlreadyExistsException(
                    "Case with id '" + caseId + "' is already added to plan with id '" + planId + "'"
            );
        }

        plan.getCases().add(testCase);
        planRepository.save(plan);
    }

    @Transactional
    public void removeCase(int planId, int caseId) {
        Plan plan = findOne(planId);

        boolean removed = plan.getCases().removeIf(c -> c.getId().equals(caseId));
        if (!removed) {
            throw new CaseNotFoundException(
                    "Case with id '" + caseId + "' is not in plan with id '" + planId + "'"
            );
        }

        planRepository.save(plan);
    }

    @Transactional
    public void delete(int id) {
        if (!planRepository.existsById(id)) {
            throw new PlanNotFoundException("Couldn't find plan with id: " + id);
        }
        planRepository.deleteById(id);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
    }

    private void ensurePlanNameIsUnique(String name, Plan currentPlan) {
        boolean duplicateExists = planRepository.existsByName(name);
        if (!duplicateExists) {
            return;
        }
        if (currentPlan != null && name.equals(currentPlan.getName())) {
            return;
        }
        throw new PlanAlreadyExistsException("Plan with name '" + name + "' already exists");
    }
}
