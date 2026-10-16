package com.example.aienglishchatbackend.llm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    private String provider = "ollama";

    private final Defaults defaults = new Defaults();

    @Getter
    @Setter
    public static class Defaults {
        private double temperature = 0.7;
        private int maxTokens = 1024;
    }
}