package ru.rt.rostelecom_tms.service.cases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.CaseGroup;
import ru.rt.rostelecom_tms.domain.cases.CaseStep;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseNotFoundException;
import ru.rt.rostelecom_tms.repository.cases.CaseRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseGroupService caseGroupService;

    public record StepCommand(Integer order, String title, String action, String expectedResult) {
    }

    public record CreateCaseCommand(
            String title,
            Integer groupId,
            String description,
            String preconditions,
            String postconditions,
            List<StepCommand> steps
    ) {
    }

    public record UpdateCaseCommand(
            String title,
            Integer groupId,
            String description,
            String preconditions,
            String postconditions,
            List<StepCommand> steps
    ) {
    }

    public List<Case> findAll() {
        return caseRepository.findAllWithStepsAndGroup();
    }

    public List<Case> findAllByGroup(int groupId) {
        caseGroupService.findOne(groupId);
        return caseRepository.findAllByGroupIdWithSteps(groupId);
    }

    public Case findOne(int id) {
        return caseRepository.findByIdWithSteps(id)
                .orElseThrow(CaseNotFoundException::new);
    }

    @Transactional
    public Case create(CreateCaseCommand cmd) {
        CaseGroup group = caseGroupService.findOne(cmd.groupId());

        Case c = new Case();
        c.setTitle(cmd.title());
        c.setGroup(group);
        c.setDescription(cmd.description());
        c.setPreconditions(cmd.preconditions());
        c.setPostconditions(cmd.postconditions());
        c.setCreatedAt(Instant.now());

        Case saved = caseRepository.save(c);

        if (cmd.steps() != null && !cmd.steps().isEmpty()) {
            List<CaseStep> steps = buildSteps(cmd.steps(), saved);
            saved.getCaseSteps().clear();
            saved.getCaseSteps().addAll(steps);
            caseRepository.save(saved);
        }

        return saved;
    }

    @Transactional
    public void update(int id, UpdateCaseCommand cmd) {
        Case c = findOne(id);

        if (cmd.title() != null) {
            c.setTitle(cmd.title());
        }
        if (cmd.groupId() != null) {
            CaseGroup group = caseGroupService.findOne(cmd.groupId());
            c.setGroup(group);
        }
        if (cmd.description() != null) {
            c.setDescription(cmd.description());
        }
        if (cmd.preconditions() != null) {
            c.setPreconditions(cmd.preconditions());
        }
        if (cmd.postconditions() != null) {
            c.setPostconditions(cmd.postconditions());
        }
        if (cmd.steps() != null) {
            c.getCaseSteps().clear();
            List<CaseStep> steps = buildSteps(cmd.steps(), c);
            c.getCaseSteps().addAll(steps);
        }

        caseRepository.save(c);
    }

    @Transactional
    public void delete(int id) {
        if (!caseRepository.existsById(id)) {
            throw new CaseNotFoundException();
        }
        caseRepository.deleteById(id);
    }

    private List<CaseStep> buildSteps(List<StepCommand> stepCommands, Case parent) {
        List<CaseStep> result = new ArrayList<>();
        for (StepCommand sc : stepCommands) {
            CaseStep step = new CaseStep();
            step.setCaseField(parent);
            step.setOrder(sc.order());
            step.setTitle(sc.title());
            step.setAction(sc.action());
            step.setExpectedResult(sc.expectedResult());
            result.add(step);
        }
        return result;
    }
}
