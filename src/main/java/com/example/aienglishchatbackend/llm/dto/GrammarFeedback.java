package com.example.aienglishchatbackend.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrammarFeedback {
    private int overallScore;
    private int grammarScore;
    private int vocabularyScore;
    private int naturalnessScore;
    private String correctedText;
    private String explanation;
}
