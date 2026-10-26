package com.example.aienglishchatbackend.websocket.handler;

import com.example.aienglishchatbackend.chat.repository.ChatRepository;
import com.example.aienglishchatbackend.llm.service.LlmService;
import com.example.aienglishchatbackend.message.entity.Message;
import com.example.aienglishchatbackend.message.repository.MessageRepository;
import com.example.aienglishchatbackend.user.repository.UserRepository;
import com.example.aienglishchatbackend.websocket.model.WebSocketErrorCode;
import com.example.aienglishchatbackend.websocket.model.WebSocketMessageType;
import com.example.aienglishchatbackend.websocket.service.WebSocketAuthService;
import com.example.aienglishchatbackend.websocket.service.WebSocketSender;
import com.example.aienglishchatbackend.websocket.session.ChatSession;
import com.example.aienglishchatbackend.websocket.session.ChatSessionRegistry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatSessionRegistry sessionRegistry;
    private final WebSocketAuthService authService;
    private final WebSocketSender webSocketSender;
    private final ObjectMapper objectMapper;
    private final ChatRepository chatRepository;
    private final LlmService llmService;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        ChatSession chatSession = new ChatSession(session);
        sessionRegistry.register(chatSession);
        log.info("WebSocket connected. Session: {}", session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatSession chatSession = sessionRegistry.get(session.getId()).orElse(null);
        if (chatSession == null) {
            log.warn("Session {} not found in registry", session.getId());
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            log.warn("Malformed JSON received from session {}: {}", session.getId(), message.getPayload());
            sendError(session, WebSocketErrorCode.MALFORMED_JSON, "Invalid JSON payload");
            return;
        }

        if (rootNode == null || !rootNode.has("type")) {
            log.warn("Missing 'type' in message from session {}: {}", session.getId(), message.getPayload());
            sendError(session, WebSocketErrorCode.INVALID_MESSAGE, "Message type is required");
            return;
        }

        String typeStr = rootNode.get("type").asText();
        WebSocketMessageType type = WebSocketMessageType.fromString(typeStr);

        if (type == null) {
            log.warn("Unknown message type '{}' from session {}", typeStr, session.getId());
            sendError(session, WebSocketErrorCode.UNKNOWN_TYPE, "Unknown message type: " + typeStr);
            return;
        }

        switch (type) {
            case AUTH -> handleAuth(chatSession, rootNode);
            case SEND_MESSAGE -> handleSendMessage(chatSession, rootNode);
            default -> {
                log.warn("Unhandled message type '{}' from session {}", type, session.getId());
                sendError(session, WebSocketErrorCode.UNKNOWN_TYPE, "Unhandled message type: " + type);
            }
        }
    }

    private void handleAuth(ChatSession chatSession, JsonNode rootNode) {
        String token = rootNode.hasNonNull("accessToken") ? rootNode.get("accessToken").asText() : null;
        authService.authenticate(chatSession, token);
    }

    private void handleSendMessage(ChatSession chatSession, JsonNode rootNode) {
        if (!chatSession.isAuthenticated()) {
            log.warn("Unauthenticated SEND_MESSAGE attempt from session {}", chatSession.getSessionId());
            sendError(chatSession.getSession(), WebSocketErrorCode.UNAUTHORIZED, "Authentication required");
            return;
        }

        Long chatId = rootNode.hasNonNull("chatId") ? rootNode.get("chatId").asLong() : null;
        if (chatId == null) {
            log.warn("Missing 'chatId' in SEND_MESSAGE from session {}", chatSession.getSessionId());
            sendError(chatSession.getSession(), WebSocketErrorCode.INVALID_MESSAGE, "chatId is required");
            return;
        }

        if (chatRepository.findByChatIdAndUserUserId(chatId, chatSession.getUserId()).isEmpty()) {
            log.warn("Access denied: User {} cannot send message to chat {}", chatSession.getUserId(), chatId);
            sendError(chatSession.getSession(), WebSocketErrorCode.FORBIDDEN, "You do not have access to this chat");
            return;
        }

        chatSession.setChatId(chatId);

        String content = rootNode.hasNonNull("content") ? rootNode.get("content").asText() : "";
        String contentType = rootNode.hasNonNull("contentType") ? rootNode.get("contentType").asText() : "TEXT";
        Long messageId = rootNode.hasNonNull("messageId") ? rootNode.get("messageId").asLong() : null;

        Message userMessage;
        if (messageId != null) {
            userMessage = messageRepository.findById(messageId).orElseThrow();
        } else {
            userMessage = new Message();
            userMessage.setContent(content);
            userMessage.setContentType(Message.ContentType.valueOf(contentType));
            userMessage.setRole(Message.Role.USER);
            userMessage.setChat(chatRepository.findById(chatId).orElseThrow());
            userMessage.setUser(userRepository.findById(chatSession.getUserId()).orElseThrow());
            messageRepository.save(userMessage);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", WebSocketMessageType.MESSAGE_ACCEPTED.getValue());
        response.put("chatId", chatSession.getChatId());
        response.put("userId", chatSession.getUserId());
        response.put("content", content);
        response.put("messageId", userMessage.getMessageId());
        response.put("contentType", contentType);
        response.put("message", "Received message from user (session " + chatSession.getSessionId() + ")");

        webSocketSender.send(chatSession.getSession(), response);

        llmService.generateReplyStreaming(
                chatId,
                content,
                token -> {
                    Map<String, Object> tokenResponse = new LinkedHashMap<>();
                    tokenResponse.put("type", WebSocketMessageType.MESSAGE_STREAM.getValue());
                    tokenResponse.put("chatId", chatSession.getChatId());
                    tokenResponse.put("token", token);
                    webSocketSender.send(chatSession.getSession(), tokenResponse);
                },
                responseObj -> {
                    Message assistantMessage = new Message();
                    assistantMessage.setContent(responseObj.getContent());
                    assistantMessage.setContentType(Message.ContentType.TEXT);
                    assistantMessage.setRole(Message.Role.ASSISTANT);
                    assistantMessage.setChat(chatRepository.findById(chatId).orElseThrow());
                    messageRepository.save(assistantMessage);
                    
                    Map<String, Object> completeResponse = new LinkedHashMap<>();
                    completeResponse.put("type", WebSocketMessageType.MESSAGE_STREAM_COMPLETED.getValue());
                    completeResponse.put("chatId", chatSession.getChatId());
                    completeResponse.put("model", responseObj.getModel());
                    completeResponse.put("finishReason", responseObj.getFinishReason());
                    webSocketSender.send(chatSession.getSession(), completeResponse);
                },
                throwable -> {
                    log.error("LLM streaming error for session {}", chatSession.getSessionId(), throwable);
                    sendError(chatSession.getSession(), WebSocketErrorCode.INVALID_MESSAGE, "LLM streaming failed");
                }
        );
    }

    private void sendError(WebSocketSession session, WebSocketErrorCode code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("type", WebSocketMessageType.ERROR.getValue());
        error.put("code", code.getCode());
        error.put("message", message);
        webSocketSender.send(session, error);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionRegistry.unregister(session.getId());
        log.info("WebSocket connection closed. Session: {}, Status: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage(), exception);
        sessionRegistry.unregister(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}
