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
import java.util.List;

import static ru.rt.rostelecom_tms.service.cases.CaseStepService.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseGroupService caseGroupService;

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

    public List<Case> findAllByPlan(int planId) {
        return caseRepository.findAllByPlanId(planId);
    }

    public Case findOne(int id) {
        return caseRepository.findByIdWithSteps(id)
                .orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + id));
    }

    @Transactional
    public Case create(CreateCaseCommand cmd) {
        validateStepCommands(cmd.steps());
        CaseGroup group = caseGroupService.findOne(cmd.groupId());
        ensureCaseTitleIsUnique(cmd.title(), group.getId(), null);

        Case newCase = new Case();
        newCase.setTitle(cmd.title());
        newCase.setGroup(group);
        newCase.setDescription(cmd.description());
        newCase.setPreconditions(cmd.preconditions());
        newCase.setPostconditions(cmd.postconditions());
        newCase.setCreatedAt(Instant.now());

        Case saved = caseRepository.save(newCase);

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
        Case existingCase = findOne(id);
        validateStepCommands(cmd.steps());

        String nextTitle = cmd.title() != null ? cmd.title() : existingCase.getTitle();
        Integer nextGroupId = cmd.groupId() != null ? cmd.groupId() : existingCase.getGroup().getId();
        ensureCaseTitleIsUnique(nextTitle, nextGroupId, existingCase);

        if (cmd.title() != null) {
            existingCase.setTitle(cmd.title());
        }
        if (cmd.groupId() != null) {
            CaseGroup group = caseGroupService.findOne(cmd.groupId());
            existingCase.setGroup(group);
        }
        if (cmd.description() != null) {
            existingCase.setDescription(cmd.description());
        }
        if (cmd.preconditions() != null) {
            existingCase.setPreconditions(cmd.preconditions());
        }
        if (cmd.postconditions() != null) {
            existingCase.setPostconditions(cmd.postconditions());
        }
        if (cmd.steps() != null) {
            existingCase.getCaseSteps().clear();
            caseRepository.saveAndFlush(existingCase);

            List<CaseStep> steps = buildSteps(cmd.steps(), existingCase);
            existingCase.getCaseSteps().addAll(steps);
        }

        caseRepository.save(existingCase);
    }

    @Transactional
    public void delete(int id) {
        if (!caseRepository.existsById(id)) {
            throw new CaseNotFoundException("Couldn't find case with id: " + id);
        }
        caseRepository.deleteById(id);
    }

    private void ensureCaseTitleIsUnique(String title, Integer groupId, Case currentCase) {
        boolean duplicateExists = caseRepository.existsByTitleAndGroupId(title, groupId);
        if (!duplicateExists) {
            return;
        }

        if (currentCase != null) {
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


}
