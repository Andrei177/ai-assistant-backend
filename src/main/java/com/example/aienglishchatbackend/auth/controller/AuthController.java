package com.example.aienglishchatbackend.auth.controller;

import com.example.aienglishchatbackend.auth.dto.internal.AuthResponseInternal;
import com.example.aienglishchatbackend.auth.dto.req.AuthRequest;
import com.example.aienglishchatbackend.auth.dto.res.AuthResponse;
import com.example.aienglishchatbackend.auth.mapper.AuthMapper;
import com.example.aienglishchatbackend.auth.service.AuthService;
import com.example.aienglishchatbackend.common.exception.InvalidCredentialsException;
import com.example.aienglishchatbackend.user.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody AuthRequest registerReq, @RequestHeader("User-Agent") String userAgent){
        AuthResponseInternal registerUserRes = authService.registerUser(registerReq, userAgent);

        ResponseCookie refreshTokenCookie = setRefreshTokenCookie(registerUserRes.refreshToken());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(AuthMapper.authResponseInternalToAuthResponse(registerUserRes));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody AuthRequest loginReq, @RequestHeader("User-Agent") String userAgent){
        AuthResponseInternal loginUserRes = authService.loginUser(loginReq, userAgent);

        ResponseCookie refreshTokenCookie = setRefreshTokenCookie(loginUserRes.refreshToken());

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(AuthMapper.authResponseInternalToAuthResponse(loginUserRes));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            @RequestHeader("User-Agent") String userAgent) {

        String refreshToken = extractRefreshTokenFromCookie(request);
        AuthResponseInternal response = authService.refresh(refreshToken, userAgent);

        ResponseCookie newRefreshCookie = setRefreshTokenCookie(response.refreshToken());

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
                .body(AuthMapper.authResponseInternalToAuthResponse(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        authService.logout(refreshToken);

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        return ResponseEntity
                .noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal User user) {
        authService.logoutAll(user);

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        return ResponseEntity
                .noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new InvalidCredentialsException("Refresh-токен не найден");
        }

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        throw new InvalidCredentialsException("Refresh-токен не найден");
    }

    private ResponseCookie setRefreshTokenCookie(String refreshToken){
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
    }
}
