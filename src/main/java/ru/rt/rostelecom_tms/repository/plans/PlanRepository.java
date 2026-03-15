package ru.rt.rostelecom_tms.repository.plans;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.plans.Plan;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {

    @Query("SELECT p FROM Plan p LEFT JOIN FETCH p.responsibleUser")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Plan> findAllWithUser();

    @Query("SELECT DISTINCT p FROM Plan p LEFT JOIN FETCH p.cases WHERE p IN :plans")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Plan> fetchCasesForPlans(@Param("plans") List<Plan> plans);

    @Query("SELECT p FROM Plan p LEFT JOIN FETCH p.cases LEFT JOIN FETCH p.responsibleUser WHERE p.id = :id")
    Optional<Plan> findByIdWithCasesAndUser(@Param("id") Integer id);

    boolean existsByName(String name);
}
