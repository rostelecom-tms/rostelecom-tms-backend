package ru.rt.rostelecom_tms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    private String provider = "openai";
    private String openaiApiKey;
    private String openaiModel = "gpt-4o-mini";
    private String ollamaBaseUrl = "http://localhost:11434";
    private String ollamaModel = "qwen3:4b";
    private boolean ollamaThink = false;
    private String thirdAiBaseUrl = "";
    private String thirdAiApiKey = "";
    private String thirdAiModel = "";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public void setOpenaiApiKey(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
    }

    public String getOpenaiModel() {
        return openaiModel;
    }

    public void setOpenaiModel(String openaiModel) {
        this.openaiModel = openaiModel;
    }

    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }

    public void setOllamaBaseUrl(String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    public void setOllamaModel(String ollamaModel) {
        this.ollamaModel = ollamaModel;
    }

    public boolean isOllamaThink() {
        return ollamaThink;
    }

    public void setOllamaThink(boolean ollamaThink) {
        this.ollamaThink = ollamaThink;
    }

    public String getThirdAiBaseUrl() {
        return thirdAiBaseUrl;
    }

    public void setThirdAiBaseUrl(String thirdAiBaseUrl) {
        this.thirdAiBaseUrl = thirdAiBaseUrl;
    }

    public String getThirdAiApiKey() {
        return thirdAiApiKey;
    }

    public void setThirdAiApiKey(String thirdAiApiKey) {
        this.thirdAiApiKey = thirdAiApiKey;
    }

    public String getThirdAiModel() {
        return thirdAiModel;
    }

    public void setThirdAiModel(String thirdAiModel) {
        this.thirdAiModel = thirdAiModel;
    }
}
