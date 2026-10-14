package com.example.aienglishchatbackend.user.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserSessionResponse {
    private Long tokenId;
    private String deviceInfo;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
