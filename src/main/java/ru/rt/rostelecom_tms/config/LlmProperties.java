package ru.rt.rostelecom_tms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    private String provider = "ollama";
    private String openaiApiKey;
    private String openaiModel = "gpt-4o-mini";
    private String ollamaBaseUrl = "http://localhost:11434";
    private String ollamaModel = "qwen3:4b";
    private boolean ollamaThink = false;
    private String thirdAiBaseUrl = "";
    private String thirdAiApiKey = "";
    private String thirdAiModel = "";
}
