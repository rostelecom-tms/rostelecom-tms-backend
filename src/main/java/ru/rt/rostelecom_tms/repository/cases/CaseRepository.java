package ru.rt.rostelecom_tms.repository.cases;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.cases.Case;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Integer> {

    @Query("SELECT DISTINCT c FROM Case c LEFT JOIN FETCH c.caseSteps LEFT JOIN FETCH c.group")
    List<Case> findAllWithStepsAndGroup();

    @Query("SELECT DISTINCT c FROM Case c LEFT JOIN FETCH c.caseSteps LEFT JOIN FETCH c.group WHERE c.group.id = :groupId")
    List<Case> findAllByGroupIdWithSteps(@Param("groupId") Integer groupId);

    @Query("SELECT c FROM Case c LEFT JOIN FETCH c.caseSteps LEFT JOIN FETCH c.group WHERE c.id = :id")
    Optional<Case> findByIdWithSteps(@Param("id") Integer id);

    boolean existsByTitleAndGroupId(String title, Integer groupId);

    boolean existsByGroupId(Integer groupId);
}
