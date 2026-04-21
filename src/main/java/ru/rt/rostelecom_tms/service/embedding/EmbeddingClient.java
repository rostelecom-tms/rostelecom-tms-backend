package ru.rt.rostelecom_tms.service.embedding;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rt.rostelecom_tms.config.EmbeddingProperties;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmbeddingClient {

    private final List<EmbeddingProviderClient> providerClients;
    private final EmbeddingProperties properties;

    private Map<EmbeddingProvider, EmbeddingProviderClient> byProvider;

    @PostConstruct
    void init() {
        byProvider = providerClients.stream()
                .collect(Collectors.toMap(
                        EmbeddingProviderClient::provider,
                        c -> c,
                        (a, b) -> a,
                        () -> new EnumMap<>(EmbeddingProvider.class)
                ));
    }

    public List<Double> embed(String text) {
        return embed(text, null);
    }

    public List<Double> embed(String text, String providerOverride) {
        EmbeddingProvider provider = EmbeddingProvider.from(
                providerOverride,
            EmbeddingProvider.from(properties.getProvider(), EmbeddingProvider.OLLAMA)
        );

        EmbeddingProviderClient client = byProvider.get(provider);
        if (client == null) {
            throw new IllegalStateException("No embedding client configured for provider: " + provider);
        }

        List<Double> vector = client.embed(text);
        validateDimensions(vector, provider);
        return vector;
    }

    private void validateDimensions(List<Double> vector, EmbeddingProvider provider) {
        if (vector == null || vector.isEmpty()) {
            throw new IllegalStateException("Embedding provider returned empty vector: " + provider.name().toLowerCase());
        }

        int expected = properties.getDimensions();
        if (expected > 0 && vector.size() != expected) {
            throw new IllegalStateException(
                    "Embedding dimensions mismatch for provider "
                            + provider.name().toLowerCase()
                            + ": expected " + expected
                            + ", got " + vector.size()
                            + ". Update app.embedding.dimensions and DB vector column dimension, then reindex data."
            );
        }
    }

    public String getDefaultProvider() {
        return EmbeddingProvider.from(properties.getProvider(), EmbeddingProvider.OLLAMA).name().toLowerCase();
    }

    public List<String> getSupportedProviders() {
        return providerClients.stream()
                .filter(client -> client.provider() != EmbeddingProvider.THIRD_AI)
                .map(client -> client.provider().name().toLowerCase())
                .sorted()
                .toList();
    }
}
