package com.example.aienglishchatbackend.llm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "llm.ollama")
public class OllamaProperties {

    private String baseUrl = "http://localhost:11434";
    private String model = "llama3.2";
}