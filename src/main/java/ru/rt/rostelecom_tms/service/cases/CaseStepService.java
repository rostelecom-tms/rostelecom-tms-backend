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
        return caseStepRepository.findAllByCaseFieldIdOrderByOrderAsc(caseId);
    }

    @Transactional
    public List<CaseStep> createCaseSteps(Integer caseId, List<StepCommand> cmd) {
        Case existingCase = caseRepository.findOneById(caseId)
                .orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + caseId));

        validateStepCommands(cmd);

        List<CaseStep> newSteps = buildSteps(cmd, existingCase);

        List<CaseStep> combined = new ArrayList<>(existingCase.getCaseSteps());
        combined.addAll(newSteps);
        validateCaseSteps(combined);

        return caseStepRepository.saveAll(newSteps);
    }

    @Transactional
    public void updateCaseStep(Integer caseStepId, UpdateCaseStepCommand cmd) {
        CaseStep caseStep = caseStepRepository.findById(caseStepId).orElseThrow(() -> new CaseStepNotFoundException("Couldn't find case step with id: " + caseStepId));

        if (cmd.order != null) {
            caseStep.setOrder(cmd.order);
        }

        if (cmd.title != null) {
            caseStep.setTitle(cmd.title);
        }

        if (cmd.action != null) {
            caseStep.setAction(cmd.action);
        }

        if (cmd.expectedResult != null) {
            caseStep.setExpectedResult(cmd.expectedResult);
        }

        validateCaseSteps(caseStep.getCaseField().getCaseSteps().stream().toList());

        caseStepRepository.save(caseStep);
    }

    @Transactional
    public void deleteCaseStep(Integer caseStepId) {
        CaseStep caseStep = caseStepRepository.findById(caseStepId).orElseThrow(() -> new CaseStepNotFoundException("Couldn't find case step with id: " + caseStepId));

        Case newCase = caseStep.getCaseField();
        newCase.getCaseSteps().remove(caseStep);

        caseRepository.save(newCase);

        caseStepRepository.delete(caseStep);
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
