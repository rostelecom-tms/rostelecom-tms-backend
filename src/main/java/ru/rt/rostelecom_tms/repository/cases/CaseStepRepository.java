package ru.rt.rostelecom_tms.repository.cases;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rt.rostelecom_tms.domain.cases.CaseStep;

import java.util.List;

public interface CaseStepRepository extends JpaRepository<CaseStep, Integer> {
    @Query("SELECT cs FROM CaseStep cs JOIN FETCH cs.caseField WHERE cs.caseField.id = :caseId ORDER BY cs.order")
    List<CaseStep> findAllByCaseId(@Param("caseId") Integer caseId);

}
