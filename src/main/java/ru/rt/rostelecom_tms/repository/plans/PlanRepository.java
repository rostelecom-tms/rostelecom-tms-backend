package ru.rt.rostelecom_tms.repository.plans;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.plans.Plan;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {

    @Query("SELECT DISTINCT p FROM Plan p LEFT JOIN FETCH p.plansCases pc LEFT JOIN FETCH pc.caseField LEFT JOIN FETCH p.responsibleUser")
    List<Plan> findAllWithCasesAndUser();

    @Query("SELECT p FROM Plan p LEFT JOIN FETCH p.plansCases pc LEFT JOIN FETCH pc.caseField LEFT JOIN FETCH p.responsibleUser WHERE p.id = :id")
    Optional<Plan> findByIdWithCasesAndUser(@Param("id") Integer id);

    boolean existsByName(String name);
}
