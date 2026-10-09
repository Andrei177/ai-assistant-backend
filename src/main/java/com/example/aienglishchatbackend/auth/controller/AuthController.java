package com.example.aienglishchatbackend.auth.controller;

import com.example.aienglishchatbackend.auth.dto.internal.AuthResponseInternal;
import com.example.aienglishchatbackend.auth.dto.req.AuthRequest;
import com.example.aienglishchatbackend.auth.dto.res.AuthResponse;
import com.example.aienglishchatbackend.auth.mapper.AuthMapper;
import com.example.aienglishchatbackend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(AuthMapper.authResponseInternalToAuthResponse(loginUserRes));
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
