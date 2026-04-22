package ru.rt.rostelecom_tms.service.embedding;

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
            IllegalStateException cause = new IllegalStateException("OPENAI_API_KEY is required for openai embedding provider");
            throw new EmbeddingProviderException(cause.getMessage(), cause);
        }

        try {
            Map<String, Object> response = client.post()
                    .uri("/v1/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getOpenaiApiKey())
                    .body(Map.of(
                            "model", properties.getOpenaiModel(),
                            "input", text
                    ))
                    .retrieve()
                    .body(Map.class);

            Object embedding = readEmbedding(response);
            return toDoubleList(embedding);
        } catch (RestClientException e) {
            log.error("OpenAI embedding request failed: {}", e.getMessage());
            throw new EmbeddingProviderException("OpenAI embedding provider unavailable: " + e.getMessage(), e);
        }
    }

    private List<Double> toDoubleList(Object embeddingRaw) {
        if (!(embeddingRaw instanceof List<?> embeddingList) || embeddingList.isEmpty()) {
            IllegalStateException cause = new IllegalStateException("OpenAI returned empty embedding");
            throw new EmbeddingProviderException(cause.getMessage(), cause);
        }

        List<Double> vector = new ArrayList<>(embeddingList.size());
        embeddingList.forEach(value -> {
            if (value instanceof Number number) {
                vector.add(number.doubleValue());
            }
        });

        if (vector.isEmpty()) {
            IllegalStateException cause = new IllegalStateException("OpenAI returned invalid embedding values");
            throw new EmbeddingProviderException(cause.getMessage(), cause);
        }

        return vector;
    }

    private Object readEmbedding(Map<String, Object> response) {
        Object dataRaw = response.get("data");
        if (!(dataRaw instanceof List<?> dataList) || dataList.isEmpty()) {
            IllegalStateException cause = new IllegalStateException("OpenAI returned empty embedding payload");
            throw new EmbeddingProviderException(cause.getMessage(), cause);
        }

        Object first = dataList.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            IllegalStateException cause = new IllegalStateException("OpenAI returned invalid embedding payload");
            throw new EmbeddingProviderException(cause.getMessage(), cause);
        }

        return firstMap.get("embedding");
    }
}
