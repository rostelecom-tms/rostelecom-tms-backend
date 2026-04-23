package ru.rt.rostelecom_tms.service.llm;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.rt.rostelecom_tms.config.LlmProperties;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiLlmProviderClient implements LlmProviderClient {

    private final LlmProperties properties;

    private RestClient client;

    @PostConstruct
    void init() {
        client = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .build();
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.OPENAI;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt, String modelOverride) {
        if (!StringUtils.hasText(properties.getOpenaiApiKey())) {
            IllegalStateException cause = new IllegalStateException("OPENAI_API_KEY is required for openai llm provider");
            throw new LlmProviderException(cause.getMessage(), cause);
        }

        String model = StringUtils.hasText(modelOverride) ? modelOverride : properties.getOpenaiModel();

        try {
            Map<String, Object> response = client.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getOpenaiApiKey())
                    .body(Map.of(
                            "model", model,
                            "temperature", 0.2,
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", userPrompt)
                            )
                    ))
                    .retrieve()
                                .body(Map.class);

                            String content = readChatContent(response);
            if (content == null || content.isBlank()) {
                IllegalStateException cause = new IllegalStateException("OpenAI returned empty completion");
                throw new LlmProviderException(cause.getMessage(), cause);
            }
            return content;
        } catch (RestClientException e) {
            log.error("OpenAI LLM request failed: {}", e.getMessage());
            throw new LlmProviderException("OpenAI LLM provider unavailable: " + e.getMessage(), e);
        }
    }

    private String readChatContent(Map<String, Object> response) {
        Object choicesRaw = response.get("choices");
        if (!(choicesRaw instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }

        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> firstChoiceMap)) {
            return null;
        }

        Object messageRaw = firstChoiceMap.get("message");
        if (!(messageRaw instanceof Map<?, ?> messageMap)) {
            return null;
        }

        Object contentRaw = messageMap.get("content");
        return contentRaw == null ? null : String.valueOf(contentRaw);
    }
}

