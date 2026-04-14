package ru.rt.rostelecom_tms.service.llm;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rt.rostelecom_tms.config.LlmProperties;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LlmClient {

    private final List<LlmProviderClient> providerClients;
    private final LlmProperties properties;

    private Map<LlmProvider, LlmProviderClient> byProvider;

    @PostConstruct
    void init() {
        byProvider = providerClients.stream()
                .collect(Collectors.toMap(
                        LlmProviderClient::provider,
                        c -> c,
                        (a, b) -> a,
                        () -> new EnumMap<>(LlmProvider.class)
                ));
    }

    public String complete(String systemPrompt, String userPrompt) {
        return complete(systemPrompt, userPrompt, null);
    }

    public String complete(String systemPrompt, String userPrompt, String providerOverride) {
        LlmProvider provider = LlmProvider.from(
                providerOverride,
                LlmProvider.from(properties.getProvider(), LlmProvider.OPENAI)
        );

        LlmProviderClient client = byProvider.get(provider);
        if (client == null) {
            throw new IllegalStateException("No llm client configured for provider: " + provider);
        }

        return client.complete(systemPrompt, userPrompt);
    }
}
