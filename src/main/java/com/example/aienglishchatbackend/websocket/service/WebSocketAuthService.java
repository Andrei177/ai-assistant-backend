package com.example.aienglishchatbackend.websocket.service;

import com.example.aienglishchatbackend.common.security.CustomUserDetailsService;
import com.example.aienglishchatbackend.common.security.JwtService;
import com.example.aienglishchatbackend.user.entity.User;
import com.example.aienglishchatbackend.websocket.session.ChatSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketAuthService {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final WebSocketSender webSocketSender;

    public boolean authenticate(ChatSession chatSession, String token) {
        try {
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("Access token is missing");
            }

            String username = jwtService.extractUsername(token);
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Invalid token payload");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtService.isTokenValid(token, userDetails)) {
                throw new IllegalArgumentException("Invalid or expired token");
            }

            if (userDetails instanceof User user) {
                chatSession.setUserId(user.getUserId());
            }

            chatSession.setAuthenticated(true);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("type", "AUTH_OK");
            response.put("userId", chatSession.getUserId());
            response.put("chatId", chatSession.getChatId());

            webSocketSender.send(chatSession.getSession(), response);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket authentication failed for session {}: {}", chatSession.getSessionId(), e.getMessage());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("type", "AUTH_FAILED");
            response.put("code", "INVALID_TOKEN");
            response.put("message", e.getMessage() != null ? e.getMessage() : "Invalid or expired token");

            webSocketSender.send(chatSession.getSession(), response);

            try {
                chatSession.getSession().close(new CloseStatus(4401, "Unauthorized"));
            } catch (IOException ioException) {
                log.error("Error closing unauthenticated session {}: {}", chatSession.getSessionId(), ioException.getMessage());
            }

            return false;
        }
    }
}
