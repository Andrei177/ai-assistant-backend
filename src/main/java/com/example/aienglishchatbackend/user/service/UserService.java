package com.example.aienglishchatbackend.user.service;

import com.example.aienglishchatbackend.auth.entity.RefreshToken;
import com.example.aienglishchatbackend.auth.repository.RefreshTokenRepository;
import com.example.aienglishchatbackend.common.exception.ResourceNotFound;
import com.example.aienglishchatbackend.user.dto.res.UserSessionResponse;
import com.example.aienglishchatbackend.user.entity.User;
import com.example.aienglishchatbackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public void deleteUser(User user){
        userRepository.delete(user);
    }

    public List<UserSessionResponse> getUserSessions(User user) {
        return refreshTokenRepository.findAllByUserAndIsRevokedFalse(user).stream()
                .map(token -> UserSessionResponse.builder()
                        .tokenId(token.getTokenId())
                        .deviceInfo(token.getDeviceInfo())
                        .createdAt(token.getCreatedAt())
                        .expiresAt(token.getExpiresAt())
                        .build())
                .collect(Collectors.toList());
    }

    public void revokeSession(User user, Long tokenId) {
        RefreshToken token = refreshTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFound("Сессия не найдена"));
        
        if (!token.getUser().getUserId().equals(user.getUserId())) {
            throw new ResourceNotFound("Сессия не найдена");
        }
        
        token.setIsRevoked(true);
        refreshTokenRepository.save(token);
    }
}
