package com.example.aienglishchatbackend.user.controller;

import com.example.aienglishchatbackend.user.dto.res.UserResponse;
import com.example.aienglishchatbackend.user.dto.res.UserSessionResponse;
import com.example.aienglishchatbackend.user.entity.User;
import com.example.aienglishchatbackend.user.mapper.UserMapper;
import com.example.aienglishchatbackend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUserInfo(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(
                UserMapper.userToUserResponse(user)
        );
    }
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal User user){
        userService.deleteUser(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/sessions")
    public ResponseEntity<List<UserSessionResponse>> getUserSessions(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(userService.getUserSessions(user));
    }

    @DeleteMapping("/me/sessions/{tokenId}")
    public ResponseEntity<Void> revokeSession(@AuthenticationPrincipal User user, @PathVariable Long tokenId){
        userService.revokeSession(user, tokenId);
        return ResponseEntity.noContent().build();
    }
}
