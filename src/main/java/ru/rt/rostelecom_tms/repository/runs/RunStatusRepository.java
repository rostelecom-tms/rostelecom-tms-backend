package ru.rt.rostelecom_tms.repository.runs;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rt.rostelecom_tms.domain.runs.RunStatus;

import java.util.Optional;

public interface RunStatusRepository extends JpaRepository<RunStatus, Integer> {
    Optional<RunStatus> findBySlug(String slug);
}
