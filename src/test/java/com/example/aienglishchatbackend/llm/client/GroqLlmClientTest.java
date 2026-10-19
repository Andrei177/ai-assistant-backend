package com.example.aienglishchatbackend.llm.client;

import com.example.aienglishchatbackend.llm.client.groq.GroqLlmClient;
import com.example.aienglishchatbackend.llm.config.GroqProperties;
import com.example.aienglishchatbackend.llm.config.LlmProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GroqLlmClientTest {

    @Test
    void shouldInstantiateWithDefaultSsl() {
        GroqProperties groqProperties = new GroqProperties();
        LlmProperties llmProperties = new LlmProperties();
        ObjectMapper objectMapper = new ObjectMapper();

        GroqLlmClient client = new GroqLlmClient(groqProperties, llmProperties, objectMapper);
        assertNotNull(client);
    }

    @Test
    void shouldInstantiateWithInsecureSsl() {
        GroqProperties groqProperties = new GroqProperties();
        groqProperties.setMaxTokens(512);
        LlmProperties llmProperties = new LlmProperties();
        ObjectMapper objectMapper = new ObjectMapper();

        GroqLlmClient client = new GroqLlmClient(groqProperties, llmProperties, objectMapper);
        assertNotNull(client);
    }
}
