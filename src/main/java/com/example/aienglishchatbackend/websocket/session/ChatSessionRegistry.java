package com.example.aienglishchatbackend.websocket.session;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ChatSessionRegistry {

    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    public void register(ChatSession chatSession) {
        if (chatSession != null && chatSession.getSessionId() != null) {
            sessions.put(chatSession.getSessionId(), chatSession);
        }
    }

    public void unregister(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    public Optional<ChatSession> get(String sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public Collection<ChatSession> getAll() {
        return sessions.values();
    }

    public List<ChatSession> findByChatId(Long chatId) {
        return sessions.values().stream()
                .filter(s -> chatId != null && chatId.equals(s.getChatId()))
                .collect(Collectors.toList());
    }

    public List<ChatSession> findByUserId(Long userId) {
        return sessions.values().stream()
                .filter(s -> userId != null && userId.equals(s.getUserId()))
                .collect(Collectors.toList());
    }
}
