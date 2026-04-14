package ru.rt.rostelecom_tms.repository.plans;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.plans.Plan;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {

    @EntityGraph(attributePaths = "responsibleUser")
    List<Plan> findAllBy();

    @EntityGraph(attributePaths = "cases")
    List<Plan> findDistinctByIdIn(List<Integer> planIds);

    @EntityGraph(attributePaths = {"cases", "responsibleUser"})
    Optional<Plan> findOneById(Integer id);

    @EntityGraph(attributePaths = "responsibleUser")
    List<Plan> findAllByResponsibleUserId(Integer userId);

    boolean existsByName(String name);
}
