package ru.rt.rostelecom_tms.service.llm;

public class LlmProviderException extends RuntimeException {
    public LlmProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
