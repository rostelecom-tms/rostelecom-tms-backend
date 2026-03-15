package ru.rt.rostelecom_tms.repository.runs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rt.rostelecom_tms.domain.runs.Run;

import java.time.Instant;
import java.util.List;

public interface RunRepository extends JpaRepository<Run, Integer> {

    @Query("SELECT r FROM Run r LEFT JOIN r.plan LEFT JOIN r.caseField")
    List<Run> findAllWithPlanAndCase();

    @Query("SELECT r FROM Run r LEFT JOIN r.plan LEFT JOIN r.caseField WHERE r.plan.id = :planId")
    List<Run> findRunsByPlanId(@Param("planId")  Integer planId);

    @Query("SELECT r from Run r LEFT JOIN r.plan LEFT JOIN r.caseField WHERE r.caseField.id = :caseId")
    List<Run> findRunsByCaseId(@Param("caseId") Integer caseId);

    @Query("SELECT r from Run r LEFT JOIN r.plan LEFT JOIN r.caseField WHERE r.status.id = :statusId")
    List<Run> findRunsByStatusId(@Param("statusId") Integer statusId);

    @Query("SELECT r from Run r LEFT JOIN r.plan LEFT JOIN r.caseField WHERE r.status.slug = :statusSlug")
    List<Run> findRunsByStatusSlug(@Param("statusSlug") String statusId);

    @Query("SELECT r from Run r LEFT JOIN r.plan LEFT JOIN r.caseField WHERE r.executedBy.id = :userId")
    List<Run> findRunsByExecutedBy(@Param("userId") Integer userId);

    @Query("SELECT r from Run r LEFT JOIN r.plan LEFT JOIN r.caseField WHERE r.executedAt > :executedFrom AND r.executedAt < :executedTo")
    List<Run> findRunsByExecutedFromAndTo(@Param("executedFrom") Instant executedFrom, @Param("executedTo") Instant executedTo);

    @Query("SELECT r FROM Run r LEFT JOIN r.plan LEFT JOIN r.caseField WHERE r.caseField.group.id = :groupId")
    List<Run> findRunsByCaseFieldGroupId(@Param("groupId") Integer groupId);
}
