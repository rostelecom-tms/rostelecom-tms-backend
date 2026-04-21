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

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final CaseEmbeddingService caseEmbeddingService;
    private final DefectEmbeddingService defectEmbeddingService;

    @Async("embeddingTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCaseIndex(CaseIndexEvent event) {
        runWithRetry("case", event.caseId(), () -> {
            if (event.deleted()) {
                caseEmbeddingService.delete(event.caseId());
            } else {
                caseEmbeddingService.index(event.caseId());
            }
        });
    }

    @Async("embeddingTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDefectIndex(DefectIndexEvent event) {
        runWithRetry("defect", event.defectId(), () -> {
            if (event.deleted()) {
                defectEmbeddingService.delete(event.defectId());
            } else {
                defectEmbeddingService.index(event.defectId());
            }
        });
    }

    private void runWithRetry(String entityType, int entityId, Runnable action) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                action.run();
                if (attempt > 1) {
                    log.info(
                            "Indexing {} {} succeeded on retry attempt {}/{}",
                            entityType,
                            entityId,
                            attempt,
                            MAX_RETRY_ATTEMPTS
                    );
                }
                return;
            } catch (Exception e) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    log.error(
                            "Failed to index {} {} after {}/{} attempts",
                            entityType,
                            entityId,
                            attempt,
                            MAX_RETRY_ATTEMPTS,
                            e
                    );
                    return;
                }

                log.warn(
                        "Failed to index {} {} on attempt {}/{}, retrying: {}",
                        entityType,
                        entityId,
                        attempt,
                        MAX_RETRY_ATTEMPTS,
                        e.getMessage()
                );
            }
        }
    }
}
