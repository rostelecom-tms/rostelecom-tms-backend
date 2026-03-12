package ru.rt.rostelecom_tms.repository.cases;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.cases.CaseGroup;

import java.util.Optional;

@Repository
public interface CaseGroupRepository extends JpaRepository<CaseGroup, Integer> {

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    Optional<CaseGroup> findBySlug(String slug);
}
