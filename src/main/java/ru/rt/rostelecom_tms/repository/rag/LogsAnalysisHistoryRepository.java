package ru.rt.rostelecom_tms.repository.rag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.rag.LogsAnalysisHistory;

import java.util.List;

@Repository
public interface LogsAnalysisHistoryRepository extends JpaRepository<LogsAnalysisHistory, Integer> {

    List<LogsAnalysisHistory> findTop30ByDefectIdOrderByCreatedAtDesc(Integer defectId);
}
