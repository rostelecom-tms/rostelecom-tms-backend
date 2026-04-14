package ru.rt.rostelecom_tms.service.llm;

import java.util.Locale;

public enum LlmProvider {
    OPENAI,
    OLLAMA,
    THIRD_AI;

    public static LlmProvider from(String value, LlmProvider fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "openai" -> OPENAI;
            case "ollama" -> OLLAMA;
            case "third-ai", "third_ai", "thirdai" -> THIRD_AI;
            default -> throw new IllegalArgumentException("Unknown llm provider: " + value);
        };
    }
}
