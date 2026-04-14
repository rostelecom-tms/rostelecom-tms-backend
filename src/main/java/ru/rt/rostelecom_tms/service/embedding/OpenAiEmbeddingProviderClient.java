package ru.rt.rostelecom_tms.service.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.rt.rostelecom_tms.config.EmbeddingProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingProviderClient implements EmbeddingProviderClient {

    private final EmbeddingProperties properties;

    private RestClient client;

    @PostConstruct
    void init() {
    client = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .build();
    }

    @Override
    public EmbeddingProvider provider() {
        return EmbeddingProvider.OPENAI;
    }

    @Override
    public List<Double> embed(String text) {
        if (!StringUtils.hasText(properties.getOpenaiApiKey())) {
            throw new IllegalStateException("OPENAI_API_KEY is required for openai embedding provider");
        }

        try {
            JsonNode response = client.post()
                    .uri("/v1/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getOpenaiApiKey())
                    .body(Map.of(
                            "model", properties.getOpenaiModel(),
                            "input", text
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode embedding = response.path("data").path(0).path("embedding");
            return toDoubleList(embedding);
        } catch (RestClientException e) {
            log.error("OpenAI embedding request failed: {}", e.getMessage());
            throw new EmbeddingProviderException("OpenAI embedding provider unavailable: " + e.getMessage(), e);
        }
    }

    private List<Double> toDoubleList(JsonNode embeddingNode) {
        if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
            throw new IllegalStateException("OpenAI returned empty embedding");
        }

        List<Double> vector = new ArrayList<>(embeddingNode.size());
        embeddingNode.forEach(value -> vector.add(value.asDouble()));
        return vector;
    }
}
