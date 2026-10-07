package com.example.aienglishchatbackend.auth.controller;

import com.example.aienglishchatbackend.auth.dto.req.RegisterRequest;
import com.example.aienglishchatbackend.auth.dto.res.RegisterResponse;
import com.example.aienglishchatbackend.auth.service.AuthService;
import com.example.aienglishchatbackend.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(@Valid @RequestBody RegisterRequest registerReq){
        User registerUser = authService.registerUser(registerReq);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new RegisterResponse(
                        registerUser.getUserId(),
                        registerUser.getUserEmail(),
                        registerUser.getCreatedAt())
        );
    }
}
