package ru.rt.rostelecom_tms.repository.cases;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.cases.CaseGroup;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseGroupRepository extends JpaRepository<CaseGroup, Integer> {

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    boolean existsByParentId(Integer parentId);

    @EntityGraph(attributePaths = {"project", "parent"})
    List<CaseGroup> findAll();

    @EntityGraph(attributePaths = {"project", "parent"})
    Optional<CaseGroup> findById(Integer id);

    @EntityGraph(attributePaths = {"project", "parent"})
    List<CaseGroup> findAllByParentId(Integer parentId);

    Optional<CaseGroup> findBySlug(String slug);
}
