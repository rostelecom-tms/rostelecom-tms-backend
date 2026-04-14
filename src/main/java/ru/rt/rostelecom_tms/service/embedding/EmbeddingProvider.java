package ru.rt.rostelecom_tms.service.embedding;

import java.util.Locale;

public enum EmbeddingProvider {
    OPENAI,
    OLLAMA,
    THIRD_AI;

    public static EmbeddingProvider from(String value, EmbeddingProvider fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "openai" -> OPENAI;
            case "ollama" -> OLLAMA;
            case "third-ai", "third_ai", "thirdai" -> THIRD_AI;
            default -> throw new IllegalArgumentException("Unknown embedding provider: " + value);
        };
    }
}
