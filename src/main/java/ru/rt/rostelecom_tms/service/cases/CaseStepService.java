package ru.rt.rostelecom_tms.service.cases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.CaseStep;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseNotFoundException;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseStepNotFoundException;
import ru.rt.rostelecom_tms.repository.cases.CaseRepository;
import ru.rt.rostelecom_tms.repository.cases.CaseStepRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CaseStepService {

    private final CaseStepRepository caseStepRepository;
    private final CaseRepository caseRepository;

    public record StepCommand(
            Integer order,
            String title,
            String action,
            String expectedResult
    ) {}

    public record UpdateCaseStepCommand(
            Integer order,
            String title,
            String action,
            String expectedResult
    ) {}

    public List<CaseStep> findAllByCaseId(Integer caseId) {
        return caseStepRepository.findAllByCaseId(caseId);
    }

    @Transactional
    public List<CaseStep> createCaseSteps(Integer caseId, List<StepCommand> cmd) {
        Case c = caseRepository.findById(caseId).orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + caseId));

        validateStepCommands(cmd);

        List<CaseStep> newCaseSteps = buildSteps(cmd, c);

        Set<CaseStep> allSteps = c.getCaseSteps();
        allSteps.addAll(newCaseSteps);

        c.setCaseSteps(allSteps);

        caseRepository.save(c);

        return caseStepRepository.saveAll(allSteps);
    }

    @Transactional
    public void updateCaseSteps(Integer caseStepId, UpdateCaseStepCommand cmd) {
        CaseStep cs = caseStepRepository.findById(caseStepId).orElseThrow(() -> new CaseStepNotFoundException("Couldn't find case step with id: " + caseStepId));

        Case c = cs.getCaseField();

        List<CaseStep> caseSteps  = new ArrayList<>(c.getCaseSteps());
        caseSteps.remove(cs);

        caseSteps.add(cs);
        validateCaseSteps(caseSteps);

        Integer newOrder = cmd.order != null ? cmd.order : cs.getOrder();
        String newTitle = cmd.title != null ? cmd.title : cs.getTitle();
        String newAction = cmd.action != null ? cmd.action : cs.getAction();
        String newExpectedResult = cmd.expectedResult != null
                ? cmd.expectedResult
                : cs.getExpectedResult();

        if (cmd.order != null) {
            cs.setOrder(newOrder);
        }

        if (cmd.title != null) {
            cs.setTitle(newTitle);
        }

        if (cmd.action != null) {
            cs.setAction(newAction);
        }

        if (cmd.expectedResult != null) {
            cs.setExpectedResult(newExpectedResult);
        }

        caseStepRepository.save(cs);
    }

    @Transactional
    public void deleteCaseSteps(Integer caseStepId) {
        CaseStep cs =  caseStepRepository.findById(caseStepId).orElseThrow(() -> new CaseStepNotFoundException("Couldn't find case step with id: " + caseStepId));

        Case c = cs.getCaseField();
        c.getCaseSteps().remove(cs);

        caseRepository.save(c);

        caseStepRepository.delete(cs);
    }

    public static List<CaseStep> buildSteps(List<StepCommand> stepCommands, Case parent) {
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

    public static void validateStepCommands(List<StepCommand> stepCommands) {
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

    public static void validateCaseSteps(List<CaseStep> caseSteps) {
        if (caseSteps == null || caseSteps.isEmpty()) {
            return;
        }

        Set<Integer> orders = new HashSet<>();
        for (CaseStep caseStep : caseSteps) {
            if (!orders.add(caseStep.getOrder())) {
                throw new IllegalArgumentException(
                        "Case steps must contain unique order values"
                );
            }
        }
    }
}
