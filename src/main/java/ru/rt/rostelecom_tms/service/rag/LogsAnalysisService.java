package ru.rt.rostelecom_tms.service.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rt.rostelecom_tms.service.llm.LlmClient;

@Service
@RequiredArgsConstructor
public class LogsAnalysisService {

    private static final int MAX_INPUT_CHARS = 2800;
    private static final int MAX_OUTPUT_CHARS = 1600;
    private static final int MAX_SENTENCES = 10;

    private final LlmClient llmClient;

    public LogsAnalysisResponse analyze(String prompt, String llmProvider) {
        String systemPrompt = "Ты senior SRE/QA. Отвечай СТРОГО на русском языке. Верни только короткий plain-text ответ с разделами: Кратко, Вероятная причина, Что проверить, Что исправить. Максимум 10 предложений. Не используй markdown, не добавляй размышления, chain-of-thought, теги <think> и служебный текст.";
        String normalizedPrompt = buildUserPrompt(prompt);
        String answer = normalizeAnswer(llmClient.complete(systemPrompt, normalizedPrompt, llmProvider));
        return new LogsAnalysisResponse(answer);
    }

    private String buildUserPrompt(String prompt) {
        String safePrompt = prompt == null ? "" : prompt.trim();
        if (safePrompt.length() > MAX_INPUT_CHARS) {
            safePrompt = safePrompt.substring(0, MAX_INPUT_CHARS);
        }
        return "Проанализируй дефект и логи ниже. Ответ строго на русском, коротко и по делу.\\n\\n" + safePrompt;
    }

    private String normalizeAnswer(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }

        String sanitized = raw
                .replaceAll("(?is)<think>.*?</think>", "")
                .replaceAll("(?im)^thinking\\.\\.\\..*$", "")
                .replaceAll("(?im)^\\.\\.\\.done thinking\\..*$", "")
                .trim();

        if (sanitized.length() > MAX_OUTPUT_CHARS) {
            sanitized = sanitized.substring(0, MAX_OUTPUT_CHARS).trim();
        }

        String[] sentences = sanitized.split("(?<=[.!?])\\\\s+");
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

    public record LogsAnalysisResponse(String answer) {}
}