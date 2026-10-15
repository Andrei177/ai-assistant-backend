package com.example.aienglishchatbackend.message.dto.res;

import com.example.aienglishchatbackend.message.entity.Message;
import java.time.LocalDateTime;

public record MessageResponse(
    Long messageId,
    String content,
    Message.ContentType contentType,
    Message.Role role,
    String transcription,
    LocalDateTime createdAt
) {}
