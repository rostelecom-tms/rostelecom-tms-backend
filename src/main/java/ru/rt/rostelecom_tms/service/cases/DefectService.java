package ru.rt.rostelecom_tms.service.cases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.Defect;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseNotFoundException;
import ru.rt.rostelecom_tms.domain.cases.exceptions.DefectNotFoundException;
import ru.rt.rostelecom_tms.repository.cases.CaseRepository;
import ru.rt.rostelecom_tms.repository.cases.DefectRepository;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DefectService {

    private final DefectRepository defectRepository;
    private final CaseRepository caseRepository;

    public record CreateDefectCommand(
        Integer caseId,
        String title,
        String description
    ) {}

    public record UpdateDefectCommand(
            String title,
            String description,
            Boolean isSolved
    ) {}

    public List<Defect> findAllByCaseId(Integer caseId) {
        if (caseId == null) {
            return defectRepository.findAllByOrderByCreatedAtDesc();
        }

        return defectRepository.findByCaseFieldIdOrderByCreatedAtDesc(caseId);
    }

    @Transactional
    public Defect create(CreateDefectCommand cmd) {

        Case parent = caseRepository.findById(cmd.caseId())
                .orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + cmd.caseId()));

        Defect defect = new Defect();

        defect.setCaseField(parent);
        defect.setTitle(cmd.title());
        defect.setDescription(cmd.description());
        defect.setIsSolved(false);
        defect.setCreatedAt(Instant.now());

        return defectRepository.save(defect);
    }

    @Transactional
    public void update(Integer id, UpdateDefectCommand cmd) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new DefectNotFoundException("Couldn't find defect with id: " + id));

        if (cmd.title() != null) {
            defect.setTitle(cmd.title());
        }

        if (cmd.description() != null) {
            defect.setDescription(cmd.description());
        }

        if (cmd.isSolved() != null) {
            defect.setIsSolved(cmd.isSolved());
        }

        defectRepository.save(defect);
    }
}
