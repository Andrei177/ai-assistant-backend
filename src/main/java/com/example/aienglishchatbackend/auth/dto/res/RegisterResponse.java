package com.example.aienglishchatbackend.auth.dto.res;

import java.time.LocalDateTime;

public record RegisterResponse(Long userId, String userEmail, LocalDateTime createdAt) {
}
