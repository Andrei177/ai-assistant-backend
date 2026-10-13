package com.example.aienglishchatbackend.auth.service;

import com.example.aienglishchatbackend.auth.entity.RefreshToken;
import com.example.aienglishchatbackend.auth.repository.RefreshTokenRepository;
import com.example.aienglishchatbackend.common.exception.InvalidCredentialsException;
import com.example.aienglishchatbackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.codec.digest.DigestUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RefreshToken saveOrUpdateRefreshToken(User user, String refreshTokenStr, String userAgent) {
        String tokenHash = hashToken(refreshTokenStr);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        return refreshTokenRepository.findByUserAndDeviceInfo(user, userAgent)
                .map(existingToken -> {
                    existingToken.setTokenHash(tokenHash);
                    existingToken.setExpiresAt(expiresAt);
                    existingToken.setIsRevoked(false);
                    return refreshTokenRepository.save(existingToken);
                })
                .orElseGet(() -> {
                    RefreshToken newWithBuilder = RefreshToken.builder()
                            .user(user)
                            .tokenHash(tokenHash)
                            .deviceInfo(userAgent)
                            .expiresAt(expiresAt)
                            .isRevoked(false)
                            .build();
                    return refreshTokenRepository.save(newWithBuilder);
                });
    }

    @Transactional(readOnly = true)
    public RefreshToken findValidToken(String refreshTokenStr) {
        String tokenHash = hashToken(refreshTokenStr);

        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Недействительный refresh-токен"));

        if (!token.isValid()) {
            throw new InvalidCredentialsException("Refresh-токен истёк или отозван");
        }

        return token;
    }

    @Transactional
    public void revokeToken(String refreshTokenStr) {
        String tokenHash = hashToken(refreshTokenStr);
        refreshTokenRepository.revokeByTokenHash(tokenHash);
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    private String hashToken(String rawToken) {
        return DigestUtils.sha256Hex(rawToken);
    }
}
