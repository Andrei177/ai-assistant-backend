package com.example.aienglishchatbackend.llm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "llm.groq")
public class GroqProperties {

    private String baseUrl = "https://api.groq.com/openai/v1";
    private String apiKey = "";
    private String model = "qwen/qwen3.8-27b";
    private Integer maxTokens;
    private String trustStorePath;
    private String trustStorePassword;
}