package ru.rt.rostelecom_tms.repository.runs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rt.rostelecom_tms.domain.runs.RunStatus;

import java.util.Optional;

public interface RunStatusRepository extends JpaRepository<RunStatus, Integer> {

    @Query("SELECT rs FROM RunStatus rs WHERE rs.slug = :slug")
    Optional<RunStatus> findBySlug(@Param("slug") String slug);
}
