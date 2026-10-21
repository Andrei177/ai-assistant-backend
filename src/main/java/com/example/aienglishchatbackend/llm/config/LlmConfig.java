package com.example.aienglishchatbackend.llm.config;

import com.example.aienglishchatbackend.llm.client.LlmClient;
import com.example.aienglishchatbackend.llm.client.gigachat.GigaChatLlmClient;
import com.example.aienglishchatbackend.llm.client.groq.GroqLlmClient;
import com.example.aienglishchatbackend.llm.client.ollama.OllamaLlmClient;
import com.example.aienglishchatbackend.llm.config.GigaChatProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties({
        LlmProperties.class,
        OllamaProperties.class,
        GroqProperties.class,
        GigaChatProperties.class
})
public class LlmConfig {

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "ollama", matchIfMissing = true)
    public LlmClient ollamaLlmClient(
            OllamaProperties ollamaProperties,
            LlmProperties llmProperties,
            ObjectMapper objectMapper
    ) {
        return new OllamaLlmClient(ollamaProperties, llmProperties, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "groq")
    public LlmClient groqLlmClient(
            GroqProperties groqProperties,
            LlmProperties llmProperties,
            ObjectMapper objectMapper
    ) {
        return new GroqLlmClient(groqProperties, llmProperties, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "gigachat")
    public LlmClient gigaChatLlmClient(
            GigaChatProperties gigaChatProperties,
            LlmProperties llmProperties,
            ObjectMapper objectMapper
    ) {
        return new GigaChatLlmClient(gigaChatProperties, llmProperties, objectMapper);
    }
}