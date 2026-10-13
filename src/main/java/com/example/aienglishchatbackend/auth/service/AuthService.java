package com.example.aienglishchatbackend.auth.service;

import com.example.aienglishchatbackend.auth.dto.internal.AuthResponseInternal;
import com.example.aienglishchatbackend.auth.dto.req.AuthRequest;
import com.example.aienglishchatbackend.auth.entity.RefreshToken;
import com.example.aienglishchatbackend.common.exception.InvalidCredentialsException;
import com.example.aienglishchatbackend.common.exception.ResourceAlreadyExist;
import com.example.aienglishchatbackend.common.exception.ResourceNotFound;
import com.example.aienglishchatbackend.common.security.JwtService;
import com.example.aienglishchatbackend.user.entity.User;
import com.example.aienglishchatbackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponseInternal registerUser(AuthRequest req, String userAgent){
        Optional<User> candidate = userRepository.findByUserEmail(req.getUserEmail());
        if(candidate.isPresent()){
            throw new ResourceAlreadyExist("Пользователь с таким email уже существует");
        }

        String hashPassword = passwordEncoder.encode(req.getPassword());

        User newUser = User.builder()
                .userEmail(req.getUserEmail())
                .hashPassword(hashPassword)
                .build();

        User savedUser = userRepository.save(newUser);

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshTokenStr = jwtService.generateRefreshToken(savedUser);

        refreshTokenService.saveOrUpdateRefreshToken(savedUser, refreshTokenStr, userAgent);

        return new AuthResponseInternal(savedUser, accessToken, refreshTokenStr);
    }

    @Transactional
    public AuthResponseInternal loginUser(AuthRequest req, String userAgent){
        User user = userRepository.findByUserEmail(req.getUserEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Неверный email или пароль"));

        if(!passwordEncoder.matches(req.getPassword(), user.getHashPassword())){
            throw new InvalidCredentialsException("Неверный email или пароль");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenStr = jwtService.generateRefreshToken(user);

        refreshTokenService.saveOrUpdateRefreshToken(user, refreshTokenStr, userAgent);

        return new AuthResponseInternal(user, accessToken, refreshTokenStr);
    }

    @Transactional
    public AuthResponseInternal refresh(String refreshTokenStr, String userAgent) {
        RefreshToken oldToken = refreshTokenService.findValidToken(refreshTokenStr);
        User user = oldToken.getUser();

        refreshTokenService.revokeToken(refreshTokenStr);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshTokenStr = jwtService.generateRefreshToken(user);

        refreshTokenService.saveOrUpdateRefreshToken(user, newRefreshTokenStr, userAgent);

        return new AuthResponseInternal(user, newAccessToken, newRefreshTokenStr);
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenService.revokeToken(refreshTokenStr);
    }

    @Transactional
    public void logoutAll(User user) {
        refreshTokenService.revokeAllUserTokens(user);
    }
}
