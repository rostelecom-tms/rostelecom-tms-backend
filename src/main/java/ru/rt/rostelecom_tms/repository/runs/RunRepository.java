package ru.rt.rostelecom_tms.repository.runs;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rt.rostelecom_tms.domain.runs.Run;

import java.time.Instant;
import java.util.List;

public interface RunRepository extends JpaRepository<Run, Integer> {

    List<Run> findByPlanId(Integer planId);

    List<Run> findByCaseFieldId(Integer caseId);

    List<Run> findByStatusId(Integer statusId);

    List<Run> findByStatusSlug(String statusId);

    List<Run> findByExecutedById(Integer userId);

    List<Run> findByExecutedAtBetween(Instant executedAtAfter, Instant executedAtBefore);

    List<Run> findByCaseFieldGroupId(Integer groupId);
}
