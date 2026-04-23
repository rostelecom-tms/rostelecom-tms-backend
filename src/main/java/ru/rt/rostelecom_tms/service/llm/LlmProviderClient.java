package ru.rt.rostelecom_tms.service.llm;

public interface LlmProviderClient {

    LlmProvider provider();

    String complete(String systemPrompt, String userPrompt, String modelOverride);
}
