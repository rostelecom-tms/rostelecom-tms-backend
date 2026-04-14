package ru.rt.rostelecom_tms.repository.cases;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.cases.Case;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Integer> {

    @EntityGraph(attributePaths = {"caseSteps", "group"})
    List<Case> findAllBy();

    @EntityGraph(attributePaths = {"caseSteps", "group"})
    List<Case> findAllByGroupId(Integer groupId);

    @EntityGraph(attributePaths = {"caseSteps", "group"})
    Optional<Case> findOneById(Integer id);

    @EntityGraph(attributePaths = "group")
    List<Case> findDistinctByPlansId(Integer planId);

    boolean existsByTitleAndGroupId(String title, Integer groupId);

    boolean existsByGroupId(Integer groupId);
}
