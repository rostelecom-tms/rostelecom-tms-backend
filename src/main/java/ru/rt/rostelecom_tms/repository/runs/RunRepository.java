package ru.rt.rostelecom_tms.repository.runs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import ru.rt.rostelecom_tms.domain.runs.Run;

import java.time.Instant;
import java.util.List;

public interface RunRepository extends JpaRepository<Run, Integer> {

    @EntityGraph(attributePaths = {"caseField", "plan", "status", "executedBy"})
    List<Run> findByPlanIdOrderByExecutedAtDesc(Integer planId);

    @EntityGraph(attributePaths = {"caseField", "plan", "status", "executedBy"})
    List<Run> findByCaseFieldIdOrderByExecutedAtDesc(Integer caseId);

    @EntityGraph(attributePaths = {"caseField", "plan", "status", "executedBy"})
    List<Run> findByStatusIdOrderByExecutedAtDesc(Integer statusId);

    @EntityGraph(attributePaths = {"caseField", "plan", "status", "executedBy"})
    List<Run> findByStatusSlugOrderByExecutedAtDesc(String statusId);

    @EntityGraph(attributePaths = {"caseField", "plan", "status", "executedBy"})
    List<Run> findByExecutedByIdOrderByExecutedAtDesc(Integer userId);

    @EntityGraph(attributePaths = {"caseField", "plan", "status", "executedBy"})
    List<Run> findByExecutedAtBetweenOrderByExecutedAtDesc(Instant executedAtAfter, Instant executedAtBefore);

    @EntityGraph(attributePaths = {"caseField", "plan", "status", "executedBy"})
    List<Run> findByCaseFieldGroupIdOrderByExecutedAtDesc(Integer groupId);

    @EntityGraph(attributePaths = {"caseField", "plan", "status", "executedBy"})
    List<Run> findAllByOrderByExecutedAtDesc();
}
