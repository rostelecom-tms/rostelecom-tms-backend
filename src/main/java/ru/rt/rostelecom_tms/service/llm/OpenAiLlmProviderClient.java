package ru.rt.rostelecom_tms.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
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
    public String complete(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(properties.getOpenaiApiKey())) {
            IllegalStateException cause = new IllegalStateException("OPENAI_API_KEY is required for openai llm provider");
            throw new LlmProviderException(cause.getMessage(), cause);
        }

        try {
            JsonNode response = client.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getOpenaiApiKey())
                    .body(Map.of(
                            "model", properties.getOpenaiModel(),
                            "temperature", 0.2,
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", userPrompt)
                            )
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            String content = response.path("choices").path(0).path("message").path("content").asText();
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
}

