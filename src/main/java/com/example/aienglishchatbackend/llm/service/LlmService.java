package com.example.aienglishchatbackend.llm.service;

import com.example.aienglishchatbackend.llm.client.LlmClient;
import com.example.aienglishchatbackend.llm.client.LlmMessage;
import com.example.aienglishchatbackend.llm.client.LlmOptions;
import com.example.aienglishchatbackend.llm.client.LlmResponse;
import com.example.aienglishchatbackend.llm.config.LlmProperties;
import com.example.aienglishchatbackend.llm.dto.GrammarFeedback;
import com.example.aienglishchatbackend.llm.prompt.PromptTemplates;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final LlmClient llmClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public void generateReplyStreaming(
            String userMessage,
            List<LlmMessage> history,
            Consumer<String> onToken,
            Consumer<LlmResponse> onComplete,
            Consumer<Throwable> onError
    ) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.builder()
                .role(LlmMessage.Role.SYSTEM)
                .content(PromptTemplates.CONVERSATION_SYSTEM)
                .build());
        if (history != null) {
            messages.addAll(history);
        }
        messages.add(LlmMessage.builder()
                .role(LlmMessage.Role.USER)
                .content(userMessage)
                .build());

        LlmOptions options = LlmOptions.builder()
                .temperature(properties.getDefaults().getTemperature())
                .maxTokens(properties.getDefaults().getMaxTokens())
                .stream(true)
                .build();

        llmClient.chatStream(messages, options, onToken, onComplete, onError);
    }

    public GrammarFeedback analyzeGrammar(String userMessage) {
        List<LlmMessage> messages = List.of(
                LlmMessage.builder()
                        .role(LlmMessage.Role.SYSTEM)
                        .content(PromptTemplates.GRAMMAR_FEEDBACK_SYSTEM)
                        .build(),
                LlmMessage.builder()
                        .role(LlmMessage.Role.USER)
                        .content(userMessage)
                        .build()
        );

        LlmOptions options = LlmOptions.builder()
                .temperature(0.2)
                .maxTokens(512)
                .stream(false)
                .build();

        LlmResponse response = llmClient.chat(messages, options);
        return parseGrammarFeedback(response.getContent());
    }

    public String translate(String text, String targetLang) {
        String userPrompt = "Target language: " + targetLang + "\n\nText:\n" + text;

        List<LlmMessage> messages = List.of(
                LlmMessage.builder()
                        .role(LlmMessage.Role.SYSTEM)
                        .content(PromptTemplates.TRANSLATE_SYSTEM)
                        .build(),
                LlmMessage.builder()
                        .role(LlmMessage.Role.USER)
                        .content(userPrompt)
                        .build()
        );

        LlmOptions options = LlmOptions.builder()
                .temperature(0.3)
                .maxTokens(1024)
                .stream(false)
                .build();

        LlmResponse response = llmClient.chat(messages, options);
        return response.getContent() != null ? response.getContent().trim() : "";
    }

    private GrammarFeedback parseGrammarFeedback(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Empty grammar feedback from LLM");
        }
        try {
            String json = raw.trim();
            if (json.startsWith("```")) {
                int firstNewline = json.indexOf('\n');
                int lastFence = json.lastIndexOf("```");
                if (firstNewline > 0 && lastFence > firstNewline) {
                    json = json.substring(firstNewline + 1, lastFence).trim();
                }
            }
            return objectMapper.readValue(json, GrammarFeedback.class);
        } catch (Exception e) {
            log.error("Failed to parse grammar feedback JSON: {}", raw, e);
            throw new IllegalStateException("Invalid grammar feedback JSON from LLM", e);
        }
    }
}