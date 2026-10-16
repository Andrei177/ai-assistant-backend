package com.example.aienglishchatbackend.llm.client;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmOptions {

    private String model;

    @Builder.Default
    private Double temperature = 0.7;

    private Integer maxTokens;
    private Double topP;

    @Builder.Default
    private boolean stream = false;

    private String[] stop;
    private Integer seed;
}