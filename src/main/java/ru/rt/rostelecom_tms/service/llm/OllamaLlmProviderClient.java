package ru.rt.rostelecom_tms.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.rt.rostelecom_tms.config.LlmProperties;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaLlmProviderClient implements LlmProviderClient {

    private final LlmProperties properties;

    private RestClient client;

    @PostConstruct
    void init() {
    client = RestClient.builder()
                .baseUrl(properties.getOllamaBaseUrl())
                .build();
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.OLLAMA;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        try {
            JsonNode response = client.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", properties.getOllamaModel(),
                            "system", systemPrompt,
                            "prompt", userPrompt,
                            "think", properties.isOllamaThink(),
                            "stream", false
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            String content = response.path("response").asText();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("Ollama returned empty completion");
            }
            return content;
        } catch (RestClientException e) {
            log.error("Ollama LLM request failed: {}", e.getMessage());
            throw new LlmProviderException("Ollama LLM provider unavailable: " + e.getMessage(), e);
        }
    }
}
