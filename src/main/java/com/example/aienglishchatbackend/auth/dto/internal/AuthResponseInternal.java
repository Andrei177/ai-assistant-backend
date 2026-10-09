package com.example.aienglishchatbackend.auth.dto.internal;

import com.example.aienglishchatbackend.user.entity.User;

public record AuthResponseInternal(User user, String accessToken, String refreshToken){
}
