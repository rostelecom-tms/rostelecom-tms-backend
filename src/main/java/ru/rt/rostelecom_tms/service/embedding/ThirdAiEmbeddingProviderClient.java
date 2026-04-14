package ru.rt.rostelecom_tms.service.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rt.rostelecom_tms.config.EmbeddingProperties;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ThirdAiEmbeddingProviderClient implements EmbeddingProviderClient {

    private final EmbeddingProperties properties;

    @Override
    public EmbeddingProvider provider() {
        return EmbeddingProvider.THIRD_AI;
    }

    @Override
    public List<Double> embed(String text) {
        throw new UnsupportedOperationException(
                "THIRD_AI provider scaffold is ready but integration is not implemented yet. Configure app.embedding.third-ai-* and add API call implementation in ThirdAiEmbeddingProviderClient"
                        + " (baseUrl=" + properties.getThirdAiBaseUrl() + ", model=" + properties.getThirdAiModel() + ")"
        );
    }
}
