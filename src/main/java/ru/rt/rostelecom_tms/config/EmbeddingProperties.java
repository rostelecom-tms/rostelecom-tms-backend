package ru.rt.rostelecom_tms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.embedding")
public class EmbeddingProperties {

        private String provider = "ollama";
        private int dimensions = 768;
        private String openaiApiKey;
        private String openaiModel = "text-embedding-3-small";
        private String ollamaBaseUrl = "http://localhost:11434";
        private String ollamaModel = "nomic-embed-text";
        private String thirdAiBaseUrl = "";
        private String thirdAiApiKey = "";
        private String thirdAiModel = "";
}
