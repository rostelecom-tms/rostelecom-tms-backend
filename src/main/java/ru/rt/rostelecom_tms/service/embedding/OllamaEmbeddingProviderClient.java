package ru.rt.rostelecom_tms.service.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.rt.rostelecom_tms.config.EmbeddingProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaEmbeddingProviderClient implements EmbeddingProviderClient {

    private final EmbeddingProperties properties;

    private RestClient client;

    @PostConstruct
    void init() {
    client = RestClient.builder()
                .baseUrl(properties.getOllamaBaseUrl())
                .build();
    }

    @Override
    public EmbeddingProvider provider() {
        return EmbeddingProvider.OLLAMA;
    }

    @Override
    public List<Double> embed(String text) {
        try {
            JsonNode response = client.post()
                    .uri("/api/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", properties.getOllamaModel(),
                            "input", text
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode embedding = response.path("embeddings").path(0);
            return toDoubleList(embedding);
        } catch (RestClientException e) {
            log.error("Ollama embedding request failed: {}", e.getMessage());
            throw new EmbeddingProviderException("Ollama embedding provider unavailable: " + e.getMessage(), e);
        }
    }

    private List<Double> toDoubleList(JsonNode embeddingNode) {
        if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
            throw new IllegalStateException("Ollama returned empty embedding");
        }

        List<Double> vector = new ArrayList<>(embeddingNode.size());
        embeddingNode.forEach(value -> vector.add(value.asDouble()));
        return vector;
    }
}
