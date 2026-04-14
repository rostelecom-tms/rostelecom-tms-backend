package ru.rt.rostelecom_tms.repository.cases;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.rt.rostelecom_tms.domain.cases.CaseStep;

import java.util.List;

public interface CaseStepRepository extends JpaRepository<CaseStep, Integer> {

    @EntityGraph(attributePaths = "caseField")
    List<CaseStep> findAllByCaseFieldIdOrderByOrderAsc(Integer caseId);
}
