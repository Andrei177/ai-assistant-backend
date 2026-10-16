package com.example.aienglishchatbackend.llm.client;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {
    private String content;         // полный текст ответа
    private String model;           // какая модель ответила
    private String finishReason;    // stop / length / ...
    private Usage usage; // promptTokens, completionTokens, totalTokens

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}
