package com.example.aienglishchatbackend.message.dto.res;

public record TranslateResponse(Long messageId, String original, String translated, String targetLanguage) {
}
