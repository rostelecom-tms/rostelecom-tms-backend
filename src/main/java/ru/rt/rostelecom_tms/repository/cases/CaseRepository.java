package ru.rt.rostelecom_tms.repository.cases;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.cases.Case;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Integer> {

    @EntityGraph(attributePaths = {"caseSteps", "group", "tags"})
    List<Case> findAllBy();

    @EntityGraph(attributePaths = {"caseSteps", "group", "tags"})
    List<Case> findAllByGroupId(Integer groupId);

    @EntityGraph(attributePaths = {"caseSteps", "group", "tags"})
    List<Case> findDistinctByGroupIdIn(List<Integer> groupIds);

    @EntityGraph(attributePaths = {"caseSteps", "group", "tags"})
    Optional<Case> findOneById(Integer id);

    @EntityGraph(attributePaths = {"caseSteps", "group", "tags", "caseSteps", "group", "tags"})
    List<Case> findAllByIdIn(List<Integer> ids);

    @EntityGraph(attributePaths = {"group", "tags"})
    List<Case> findDistinctByPlansId(Integer planId);

    @Query("SELECT c FROM Case c LEFT JOIN FETCH c.caseSteps WHERE c.id = :id")
    Optional<Case> findByIdWithSteps(@Param("id") Integer id);


    boolean existsByTitleAndGroupId(String title, Integer groupId);

    boolean existsByGroupId(Integer groupId);
}