package ru.rt.rostelecom_tms.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.rt.rostelecom_tms.service.search.CaseEmbeddingService;
import ru.rt.rostelecom_tms.service.search.DefectEmbeddingService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingIndexListener {

    private final CaseEmbeddingService caseEmbeddingService;
    private final DefectEmbeddingService defectEmbeddingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCaseIndex(CaseIndexEvent event) {
        try {
            if (event.deleted()) {
                caseEmbeddingService.delete(event.caseId());
            } else {
                caseEmbeddingService.index(event.caseId());
            }
        } catch (Exception e) {
            log.error("Failed to index case {}: {}", event.caseId(), e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDefectIndex(DefectIndexEvent event) {
        try {
            if (event.deleted()) {
                defectEmbeddingService.delete(event.defectId());
            } else {
                defectEmbeddingService.index(event.defectId());
            }
        } catch (Exception e) {
            log.error("Failed to index defect {}: {}", event.defectId(), e.getMessage());
        }
    }
}
