package com.example.aienglishchatbackend;

import com.example.aienglishchatbackend.auth.dto.internal.AuthResponseInternal;
import com.example.aienglishchatbackend.auth.dto.req.AuthRequest;
import com.example.aienglishchatbackend.auth.service.AuthService;
import com.example.aienglishchatbackend.auth.service.RefreshTokenService;
import com.example.aienglishchatbackend.common.exception.InvalidCredentialsException;
import com.example.aienglishchatbackend.common.exception.ResourceAlreadyExist;
import com.example.aienglishchatbackend.common.security.JwtService;
import com.example.aienglishchatbackend.user.entity.User;
import com.example.aienglishchatbackend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private AuthRequest authRequest;
    private User mockUser;
    private final String userAgent = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        authRequest = new AuthRequest("test@example.com", "correct_password");

        mockUser = new User();
        mockUser.setUserEmail("test@example.com");
        mockUser.setHashPassword("encoded_password_in_db");
    }

    // ==========================================
    // ТЕСТЫ АВТОРИЗАЦИИ (LOGIN)
    // ==========================================

    @Test
    void loginUser_Success() {
        // Arrange
        when(userRepository.findByUserEmail(authRequest.getUserEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(authRequest.getPassword(), mockUser.getHashPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(mockUser)).thenReturn("mock-access-token");
        when(jwtService.generateRefreshToken(mockUser)).thenReturn("mock-refresh-token");

        // Act
        AuthResponseInternal response = authService.loginUser(authRequest, userAgent);

        // Assert
        assertNotNull(response);
        assertEquals("mock-access-token", response.accessToken());
        assertEquals("mock-refresh-token", response.refreshToken());
        assertEquals(mockUser, response.user());

        verify(refreshTokenService, times(1)).saveOrUpdateRefreshToken(mockUser, "mock-refresh-token", userAgent);
    }

    @Test
    void loginUser_WrongPassword_ThrowsInvalidCredentialsException() {
        // Arrange
        when(userRepository.findByUserEmail(authRequest.getUserEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(authRequest.getPassword(), mockUser.getHashPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            authService.loginUser(authRequest, userAgent);
        });

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).saveOrUpdateRefreshToken(any(), any(), any());
    }

    @Test
    void loginUser_UserNotFound_ThrowsInvalidCredentialsException() {
        // Arrange
        when(userRepository.findByUserEmail(authRequest.getUserEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            authService.loginUser(authRequest, userAgent);
        });
    }

    // ==========================================
    // ТЕСТЫ РЕГИСТРАЦИИ (REGISTER)
    // ==========================================

    @Test
    void registerUser_Success() {
        // Arrange
        // Симулируем, что кандидат с таким email НЕ найден (email свободен)
        when(userRepository.findByUserEmail(authRequest.getUserEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(authRequest.getPassword())).thenReturn("encoded_password_in_db");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("mock-access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("mock-refresh-token");

        // Act
        AuthResponseInternal response = authService.registerUser(authRequest, userAgent);

        // Assert
        assertNotNull(response);
        assertEquals("mock-access-token", response.accessToken());
        assertEquals("mock-refresh-token", response.refreshToken());
        assertEquals(mockUser, response.user());

        // Проверяем вызовы обязательных методов сохранения данных
        verify(userRepository, times(1)).save(any(User.class));
        verify(refreshTokenService, times(1)).saveOrUpdateRefreshToken(any(User.class), eq("mock-refresh-token"), eq(userAgent));
    }

    @Test
    void registerUser_EmailAlreadyExists_ThrowsResourceAlreadyExist() {
        // Arrange
        // Симулируем, что кандидат с таким email УЖЕ есть в базе данных
        when(userRepository.findByUserEmail(authRequest.getUserEmail())).thenReturn(Optional.of(mockUser));

        // Act & Assert
        assertThrows(ResourceAlreadyExist.class, () -> {
            authService.registerUser(authRequest, userAgent);
        });

        // Проверяем защиту: если пользователь уже есть, шифрование и сохранение вызываться не должны!
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).saveOrUpdateRefreshToken(any(), any(), any());
    }
}
