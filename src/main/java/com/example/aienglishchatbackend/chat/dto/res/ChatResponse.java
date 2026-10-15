package com.example.aienglishchatbackend.chat.dto.res;

import java.time.LocalDateTime;

public record ChatResponse(Long chatId, String chatName, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
