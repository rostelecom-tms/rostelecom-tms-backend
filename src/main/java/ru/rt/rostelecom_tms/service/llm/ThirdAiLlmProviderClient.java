package ru.rt.rostelecom_tms.service.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rt.rostelecom_tms.config.LlmProperties;

@Component
@RequiredArgsConstructor
public class ThirdAiLlmProviderClient implements LlmProviderClient {

    private final LlmProperties properties;

    @Override
    public LlmProvider provider() {
        return LlmProvider.THIRD_AI;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt, String modelOverride) {
        throw new UnsupportedOperationException(
                "THIRD_AI LLM provider scaffold is ready but integration is not implemented yet. Configure app.llm.third-ai-* and add API call implementation in ThirdAiLlmProviderClient"
                        + " (baseUrl=" + properties.getThirdAiBaseUrl() + ", model=" + properties.getThirdAiModel() + ")"
        );
    }
}
