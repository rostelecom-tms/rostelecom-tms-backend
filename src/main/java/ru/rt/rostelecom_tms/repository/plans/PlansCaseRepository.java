package ru.rt.rostelecom_tms.repository.plans;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.plans.PlansCase;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlansCaseRepository extends JpaRepository<PlansCase, Integer> {

    @Query("SELECT pc FROM PlansCase pc LEFT JOIN FETCH pc.caseField LEFT JOIN FETCH pc.plan WHERE pc.plan.id = :planId")
    List<PlansCase> findAllByPlanIdWithCase(@Param("planId") Integer planId);

    @Query("SELECT pc FROM PlansCase pc LEFT JOIN FETCH pc.caseField LEFT JOIN FETCH pc.plan WHERE pc.id = :id")
    Optional<PlansCase> findByIdWithDetails(@Param("id") Integer id);

    boolean existsByPlanIdAndCaseFieldId(Integer planId, Integer caseId);
}
