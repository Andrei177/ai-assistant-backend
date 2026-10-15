package com.example.aienglishchatbackend.chat.dto.res;

import java.time.LocalDateTime;

public record CreateChatResponse(Long chatId, String chatName, LocalDateTime createdAt) {
}
