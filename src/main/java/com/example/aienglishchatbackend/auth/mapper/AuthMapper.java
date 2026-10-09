package com.example.aienglishchatbackend.auth.mapper;

import com.example.aienglishchatbackend.auth.dto.internal.AuthResponseInternal;
import com.example.aienglishchatbackend.auth.dto.res.AuthResponse;

public class AuthMapper {
    public static AuthResponse authResponseInternalToAuthResponse(AuthResponseInternal dto){
        return new AuthResponse(
                new AuthResponse.UserAuthResponse(
                        dto.user().getUserId(),
                        dto.user().getUserEmail(),
                        dto.user().getCreatedAt()
                ),
                dto.accessToken()
        );
    }
}
