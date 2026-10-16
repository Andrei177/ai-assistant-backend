package com.example.aienglishchatbackend.websocket.session;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

@Getter
@Setter
public class ChatSession {

    private final WebSocketSession session;
    private Long chatId;
    private Long userId;
    private volatile boolean authenticated;

    public ChatSession(WebSocketSession session) {
        this.session = session;
        this.authenticated = false;
    }

    public String getSessionId() {
        return session != null ? session.getId() : null;
    }

    public boolean isOpen() {
        return session != null && session.isOpen();
    }
}
