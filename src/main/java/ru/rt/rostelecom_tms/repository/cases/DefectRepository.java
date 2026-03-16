package ru.rt.rostelecom_tms.repository.cases;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.cases.Defect;

import java.util.List;
import java.util.Optional;

@Repository
public interface DefectRepository extends JpaRepository<Defect, Integer> {

    List<Defect> findByCaseFieldIdOrderByCreatedAtDesc(Integer caseId);

}
