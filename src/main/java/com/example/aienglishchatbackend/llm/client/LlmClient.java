package com.example.aienglishchatbackend.llm.client;

import java.util.List;
import java.util.function.Consumer;

public interface LlmClient {

    // Полный ответ одним куском (для feedback, translate и т.п.)
    LlmResponse chat(List<LlmMessage> messages, LlmOptions options);

    // Стриминг (для ответа ассистента в WebSocket)
    void chatStream(
            List<LlmMessage> messages,
            LlmOptions options,
            Consumer<String> onToken,           // каждый кусок текста
            Consumer<LlmResponse> onComplete,   // финальный ответ + usage
            Consumer<Throwable> onError
    );
}