package ru.rt.rostelecom_tms.service.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.rag.LogsAnalysisHistory;
import ru.rt.rostelecom_tms.repository.rag.LogsAnalysisHistoryRepository;
import ru.rt.rostelecom_tms.service.llm.LlmClient;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogsAnalysisService {

    private static final int MAX_INPUT_CHARS = 2800;
    private static final int MAX_OUTPUT_CHARS = 1600;
    private static final int MAX_SENTENCES = 10;
    private static final String[] PREAMBLE_PREFIXES = new String[] {
            "хорошо",
            "давай",
            "сейчас",
            "мне нужно",
            "я проанализирую",
            "проанализирую"
    };

    private final LlmClient llmClient;
    private final LogsAnalysisHistoryRepository logsAnalysisHistoryRepository;

    @Transactional
    public LogsAnalysisResponse analyze(Integer defectId, String prompt, String llmProvider, String llmModel, boolean saveHistory) {
        String systemPrompt = "Ты senior SRE/QA. Отвечай строго на русском. Верни сразу анализ логов без вступлений и пояснений: ровно 10 коротких предложений по делу. Не используй markdown, chain-of-thought, теги <think>, фразы вроде 'Хорошо'/'Давай посмотрим'.";
        String rawPrompt = normalizeRawPrompt(prompt);
        String normalizedPrompt = buildUserPrompt(rawPrompt);
        String answer = normalizeAnswer(llmClient.complete(systemPrompt, normalizedPrompt, llmProvider, llmModel));

        if (saveHistory) {
            LogsAnalysisHistory history = new LogsAnalysisHistory();
            history.setDefectId(defectId);
            history.setLogsText(rawPrompt);
            history.setAnswerText(answer);
            history.setLlmProvider(llmProvider);
            history.setLlmModel(llmModel);
            history.setCreatedAt(Instant.now());
            logsAnalysisHistoryRepository.save(history);
        }

        return new LogsAnalysisResponse(answer);
    }

    public List<LogsAnalysisHistoryItem> history(Integer defectId) {
        return logsAnalysisHistoryRepository.findTop30ByDefectIdOrderByCreatedAtDesc(defectId).stream()
                .map(item -> new LogsAnalysisHistoryItem(
                        item.getId(),
                        item.getDefectId(),
                        item.getLogsText(),
                        item.getAnswerText(),
                        item.getLlmProvider(),
                        item.getLlmModel(),
                        item.getCreatedAt()
                ))
                .toList();
    }

    private String buildUserPrompt(String prompt) {
        return "Проанализируй дефект и логи ниже. Сразу дай ровно 10 коротких предложений на русском, без вступлений.\n\n" + prompt;
    }

    private String normalizeRawPrompt(String prompt) {
        String safePrompt = prompt == null ? "" : prompt.trim();
        if (safePrompt.length() > MAX_INPUT_CHARS) {
            safePrompt = safePrompt.substring(0, MAX_INPUT_CHARS);
        }
        return safePrompt;
    }

    private String normalizeAnswer(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }

        String sanitized = raw
                .replaceAll("(?is)<think>.*?</think>", "")
                .replaceAll("(?im)^thinking\\.\\.\\..*$", "")
                .replaceAll("(?im)^\\.\\.\\.done thinking\\..*$", "")
            .replaceAll("(?is)^\\s*(хорошо|окей|ладно|давай)[^\\n.!?]*[.!?]?\\s*", "")
                .trim();

        sanitized = removePreambleLines(sanitized);

        if (sanitized.length() > MAX_OUTPUT_CHARS) {
            sanitized = sanitized.substring(0, MAX_OUTPUT_CHARS).trim();
        }

        String[] sentences = sanitized.split("(?<=[.!?])\\s+");
        if (sentences.length > MAX_SENTENCES) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < MAX_SENTENCES; i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(sentences[i].trim());
            }
            sanitized = sb.toString().trim();
        }

        return sanitized;
    }

    private String removePreambleLines(String text) {
        String[] lines = text.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String lower = trimmed.toLowerCase(Locale.ROOT);
            boolean isPreamble = false;
            for (String prefix : PREAMBLE_PREFIXES) {
                if (lower.startsWith(prefix)) {
                    isPreamble = true;
                    break;
                }
            }
            if (isPreamble) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(trimmed);
        }

        String cleaned = sb.toString().trim();
        return cleaned.isEmpty() ? text : cleaned;
    }

    public record LogsAnalysisResponse(String answer) {}

    public record LogsAnalysisHistoryItem(
            Integer id,
            Integer defectId,
            String logs,
            String answer,
            String llmProvider,
            String llmModel,
            Instant createdAt
    ) {}
}