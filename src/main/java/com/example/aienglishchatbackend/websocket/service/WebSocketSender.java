package com.example.aienglishchatbackend.websocket.service;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSender {

    private final ObjectMapper objectMapper;

    public void send(WebSocketSession session, Object payload) {
        if (session == null || !session.isOpen()) {
            log.warn("Cannot send message: WebSocket session is closed or null");
            return;
        }

        try {
            String json = payload instanceof String text ? text : objectMapper.writeValueAsString(payload);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.error("Failed to send WebSocket message to session {}: {}", session.getId(), e.getMessage(), e);
        }
    }
}
