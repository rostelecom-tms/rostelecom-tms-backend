package ru.rt.rostelecom_tms.repository.cases;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.cases.CaseTag;

import java.util.Collection;
import java.util.List;

@Repository
public interface CaseTagRepository extends JpaRepository<CaseTag, Integer> {

    List<CaseTag> findAllByNameIn(Collection<String> names);
}
