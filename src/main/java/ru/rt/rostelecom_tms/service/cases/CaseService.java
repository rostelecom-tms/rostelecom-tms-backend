package ru.rt.rostelecom_tms.service.cases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.CaseGroup;
import ru.rt.rostelecom_tms.domain.cases.CaseStep;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseAlreadyExistsException;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseNotFoundException;
import ru.rt.rostelecom_tms.repository.cases.CaseRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        validateStepCommands(cmd.steps());
        CaseGroup group = caseGroupService.findOne(cmd.groupId());
        ensureCaseTitleIsUnique(cmd.title(), group.getId(), null);

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
        validateStepCommands(cmd.steps());

        String nextTitle = cmd.title() != null ? cmd.title() : c.getTitle();
        Integer nextGroupId = cmd.groupId() != null ? cmd.groupId() : c.getGroup().getId();
        ensureCaseTitleIsUnique(nextTitle, nextGroupId, c.getId());

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

    private void ensureCaseTitleIsUnique(String title, Integer groupId, Integer currentCaseId) {
        boolean duplicateExists = caseRepository.existsByTitleAndGroupId(title, groupId);
        if (!duplicateExists) {
            return;
        }

        if (currentCaseId != null) {
            Case currentCase = findOne(currentCaseId);
            boolean sameTitle = title.equals(currentCase.getTitle());
            boolean sameGroup = groupId.equals(currentCase.getGroup().getId());
            if (sameTitle && sameGroup) {
                return;
            }
        }

        throw new CaseAlreadyExistsException(
                "Case with title '" + title + "' already exists in group with id '" + groupId + "'"
        );
    }

    private void validateStepCommands(List<StepCommand> stepCommands) {
        if (stepCommands == null || stepCommands.isEmpty()) {
            return;
        }

        Set<Integer> orders = new HashSet<>();
        for (StepCommand stepCommand : stepCommands) {
            if (!orders.add(stepCommand.order())) {
                throw new IllegalArgumentException(
                        "Case steps must contain unique order values"
                );
            }
        }
    }
}
