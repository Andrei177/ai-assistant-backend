package com.example.aienglishchatbackend.user.dto.res;

import java.time.LocalDateTime;

public record UserResponse(Long userId, String userEmail, LocalDateTime createdAt) {
}
