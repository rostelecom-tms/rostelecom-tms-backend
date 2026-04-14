package ru.rt.rostelecom_tms.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({EmbeddingProperties.class, LlmProperties.class})
public class EmbeddingConfig {
}
