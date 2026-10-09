package com.example.aienglishchatbackend.auth.dto.res;

import java.time.LocalDateTime;

public record AuthResponse(UserAuthResponse user, String accessToken) {
    public record UserAuthResponse(Long userId, String userEmail, LocalDateTime createdAt){

    }
}
