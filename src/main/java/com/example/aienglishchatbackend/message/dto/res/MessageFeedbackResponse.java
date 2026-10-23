package com.example.aienglishchatbackend.message.dto.res;

public record MessageFeedbackResponse(Long messageId, Short overallScore, Short grammarScore, Short vocabularyScore, Short naturalnessScore, String correctedText, String explanation) {
}