package com.example.aienglishchatbackend;

import com.example.aienglishchatbackend.auth.dto.internal.AuthResponseInternal;
import com.example.aienglishchatbackend.auth.entity.RefreshToken;
import com.example.aienglishchatbackend.auth.service.AuthService;
import com.example.aienglishchatbackend.auth.service.RefreshTokenService;
import com.example.aienglishchatbackend.common.exception.InvalidCredentialsException;
import com.example.aienglishchatbackend.common.security.JwtService;
import com.example.aienglishchatbackend.user.entity.User;
import com.example.aienglishchatbackend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — refresh / logout / logoutAll")
class AuthServiceTokenOperationsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RefreshToken validRefreshToken;
    private final String rawRefreshToken = "raw-refresh-token-uuid-12345";
    private final String userAgent = "Mozilla/5.0 (Test)";
    private final String newAccessToken = "new.access.token";
    private final String newRefreshToken = "new-refresh-token-uuid-67890";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .userEmail("test@example.com")
                .hashPassword("encoded-password")
                .build();

        validRefreshToken = RefreshToken.builder()
                .tokenId(10L)
                .user(testUser)
                .tokenHash("some-hash")
                .deviceInfo(userAgent)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isRevoked(false)
                .build();
    }

    // ==================== refresh ====================

    @Nested
    @DisplayName("refresh()")
    class RefreshTests {

        @Test
        @DisplayName("успешный refresh: возвращает новые токены и делает rotation")
        void refresh_success_returnsNewTokensAndRotates() {
            // given
            when(refreshTokenService.findValidToken(rawRefreshToken)).thenReturn(validRefreshToken);
            when(jwtService.generateAccessToken(testUser)).thenReturn(newAccessToken);
            when(jwtService.generateRefreshToken(testUser)).thenReturn(newRefreshToken);
            when(refreshTokenService.saveOrUpdateRefreshToken(eq(testUser), eq(newRefreshToken), eq(userAgent)))
                    .thenReturn(validRefreshToken); // возвращаемое значение не используется

            // when
            AuthResponseInternal result = authService.refresh(rawRefreshToken, userAgent);

            // then
            assertThat(result).isNotNull();
            assertThat(result.user()).isEqualTo(testUser);
            assertThat(result.accessToken()).isEqualTo(newAccessToken);
            assertThat(result.refreshToken()).isEqualTo(newRefreshToken);

            // проверяем порядок и факт вызовов (rotation)
            verify(refreshTokenService).findValidToken(rawRefreshToken);
            verify(refreshTokenService).revokeToken(rawRefreshToken);
            verify(jwtService).generateAccessToken(testUser);
            verify(jwtService).generateRefreshToken(testUser);
            verify(refreshTokenService).saveOrUpdateRefreshToken(testUser, newRefreshToken, userAgent);

            verifyNoMoreInteractions(refreshTokenService, jwtService);
        }

        @Test
        @DisplayName("невалидный/отозванный/истёкший refresh-токен → InvalidCredentialsException")
        void refresh_invalidToken_throwsException() {
            // given
            when(refreshTokenService.findValidToken(rawRefreshToken))
                    .thenThrow(new InvalidCredentialsException("Refresh-токен истёк или отозван"));

            // when / then
            assertThatThrownBy(() -> authService.refresh(rawRefreshToken, userAgent))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("истёк или отозван");

            verify(refreshTokenService).findValidToken(rawRefreshToken);
            verify(refreshTokenService, never()).revokeToken(anyString());
            verify(jwtService, never()).generateAccessToken(any());
            verify(jwtService, never()).generateRefreshToken(any());
            verify(refreshTokenService, never()).saveOrUpdateRefreshToken(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("токен не найден в БД → InvalidCredentialsException")
        void refresh_tokenNotFound_throwsException() {
            // given
            when(refreshTokenService.findValidToken(rawRefreshToken))
                    .thenThrow(new InvalidCredentialsException("Недействительный refresh-токен"));

            // when / then
            assertThatThrownBy(() -> authService.refresh(rawRefreshToken, userAgent))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Недействительный");

            verify(refreshTokenService).findValidToken(rawRefreshToken);
            verifyNoMoreInteractions(refreshTokenService);
            verifyNoInteractions(jwtService);
        }
    }

    // ==================== logout ====================

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("успешный logout: отзывает переданный refresh-токен")
        void logout_success_revokesToken() {
            // given
            doNothing().when(refreshTokenService).revokeToken(rawRefreshToken);

            // when
            authService.logout(rawRefreshToken);

            // then
            verify(refreshTokenService).revokeToken(rawRefreshToken);
            verifyNoMoreInteractions(refreshTokenService);
            verifyNoInteractions(jwtService, userRepository);
        }

        @Test
        @DisplayName("logout с несуществующим токеном — не падает (идемпотентность)")
        void logout_nonExistentToken_doesNotThrow() {
            // given
            // revokeToken внутри себя просто делает update, даже если строк 0 — ок
            doNothing().when(refreshTokenService).revokeToken(anyString());

            // when / then
            authService.logout("non-existent-token");

            verify(refreshTokenService).revokeToken("non-existent-token");
        }
    }

    // ==================== logoutAll ====================

    @Nested
    @DisplayName("logoutAll()")
    class LogoutAllTests {

        @Test
        @DisplayName("успешный logoutAll: отзывает все токены пользователя")
        void logoutAll_success_revokesAllUserTokens() {
            // given
            doNothing().when(refreshTokenService).revokeAllUserTokens(testUser);

            // when
            authService.logoutAll(testUser);

            // then
            verify(refreshTokenService).revokeAllUserTokens(testUser);
            verifyNoMoreInteractions(refreshTokenService);
            verifyNoInteractions(jwtService, userRepository);
        }
    }
}