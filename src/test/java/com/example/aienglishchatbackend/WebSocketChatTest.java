package com.example.aienglishchatbackend;

import com.example.aienglishchatbackend.chat.entity.Chat;
import com.example.aienglishchatbackend.chat.repository.ChatRepository;
import com.example.aienglishchatbackend.common.security.CustomUserDetailsService;
import com.example.aienglishchatbackend.common.security.JwtService;
import com.example.aienglishchatbackend.user.entity.User;
import com.example.aienglishchatbackend.websocket.handler.ChatWebSocketHandler;
import com.example.aienglishchatbackend.websocket.model.WebSocketErrorCode;
import com.example.aienglishchatbackend.websocket.model.WebSocketMessageType;
import com.example.aienglishchatbackend.websocket.service.WebSocketAuthService;
import com.example.aienglishchatbackend.websocket.service.WebSocketSender;
import com.example.aienglishchatbackend.websocket.session.ChatSession;
import com.example.aienglishchatbackend.websocket.session.ChatSessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketChatTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private WebSocketSession webSocketSession;

    @Mock
    private ChatRepository chatRepository;

    private ObjectMapper objectMapper;
    private ChatSessionRegistry sessionRegistry;
    private WebSocketSender webSocketSender;
    private WebSocketAuthService authService;
    private ChatWebSocketHandler chatWebSocketHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sessionRegistry = new ChatSessionRegistry();
        webSocketSender = spy(new WebSocketSender(objectMapper));
        authService = new WebSocketAuthService(jwtService, userDetailsService, webSocketSender);
        chatWebSocketHandler = new ChatWebSocketHandler(sessionRegistry, authService, webSocketSender, objectMapper, chatRepository);

        lenient().when(webSocketSession.getId()).thenReturn("test-session-1");
        lenient().when(webSocketSession.isOpen()).thenReturn(true);
    }

    @Test
    void connectionEstablished_registersSessionWithUnauthenticatedStatus() throws Exception {
        chatWebSocketHandler.afterConnectionEstablished(webSocketSession);

        ChatSession chatSession = sessionRegistry.get("test-session-1").orElse(null);
        assertNotNull(chatSession);
        assertNull(chatSession.getChatId());
        assertFalse(chatSession.isAuthenticated());
        assertNull(chatSession.getUserId());
    }

    @Test
    void authMessage_success_setsAuthenticatedAndSendsAuthOk() throws Exception {
        chatWebSocketHandler.afterConnectionEstablished(webSocketSession);

        User mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setUserEmail("user@example.com");

        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(mockUser);
        when(jwtService.isTokenValid("valid-token", mockUser)).thenReturn(true);

        String authJson = "{\"type\":\"AUTH\",\"accessToken\":\"valid-token\"}";
        chatWebSocketHandler.handleTextMessage(webSocketSession, new TextMessage(authJson));

        ChatSession chatSession = sessionRegistry.get("test-session-1").orElseThrow();
        assertTrue(chatSession.isAuthenticated());
        assertEquals(1L, chatSession.getUserId());

        ArgumentCaptor<Map<String, Object>> responseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webSocketSender, atLeastOnce()).send(eq(webSocketSession), responseCaptor.capture());

        Map<String, Object> response = responseCaptor.getValue();
        assertEquals(WebSocketMessageType.AUTH_OK.getValue(), response.get("type"));
        assertEquals(1L, response.get("userId"));
    }

    @Test
    void authMessage_invalidToken_sendsAuthFailedAndClosesSession() throws Exception {
        chatWebSocketHandler.afterConnectionEstablished(webSocketSession);

        when(jwtService.extractUsername("invalid-token")).thenThrow(new RuntimeException("Expired token"));

        String authJson = "{\"type\":\"AUTH\",\"accessToken\":\"invalid-token\"}";
        chatWebSocketHandler.handleTextMessage(webSocketSession, new TextMessage(authJson));

        ChatSession chatSession = sessionRegistry.get("test-session-1").orElseThrow();
        assertFalse(chatSession.isAuthenticated());

        ArgumentCaptor<Map<String, Object>> responseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webSocketSender, atLeastOnce()).send(eq(webSocketSession), responseCaptor.capture());

        Map<String, Object> response = responseCaptor.getValue();
        assertEquals(WebSocketMessageType.AUTH_FAILED.getValue(), response.get("type"));
        assertEquals(WebSocketErrorCode.INVALID_TOKEN.getCode(), response.get("code"));

        ArgumentCaptor<CloseStatus> statusCaptor = ArgumentCaptor.forClass(CloseStatus.class);
        verify(webSocketSession, times(1)).close(statusCaptor.capture());
        assertEquals(4401, statusCaptor.getValue().getCode());
    }

    @Test
    void sendMessage_authenticated_sendsMessageAccepted() throws Exception {
        chatWebSocketHandler.afterConnectionEstablished(webSocketSession);

        ChatSession chatSession = sessionRegistry.get("test-session-1").orElseThrow();
        chatSession.setAuthenticated(true);
        chatSession.setUserId(1L);

        when(chatRepository.findByChatIdAndUserUserId(12L, 1L)).thenReturn(Optional.of(new Chat()));

        String messageJson = "{\"type\":\"SEND_MESSAGE\",\"chatId\":12,\"content\":\"Hello\",\"contentType\":\"TEXT\"}";
        chatWebSocketHandler.handleTextMessage(webSocketSession, new TextMessage(messageJson));

        ArgumentCaptor<Map<String, Object>> responseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webSocketSender, atLeastOnce()).send(eq(webSocketSession), responseCaptor.capture());

        Map<String, Object> response = responseCaptor.getValue();
        assertEquals(WebSocketMessageType.MESSAGE_ACCEPTED.getValue(), response.get("type"));
        assertEquals(12L, response.get("chatId"));
        assertEquals(1L, response.get("userId"));
        assertEquals("Hello", response.get("content"));
        assertEquals("TEXT", response.get("contentType"));
        assertEquals("Received message from user (session test-session-1)", response.get("message"));
    }

    @Test
    void sendMessage_forbidden_sendsForbiddenError() throws Exception {
        chatWebSocketHandler.afterConnectionEstablished(webSocketSession);

        ChatSession chatSession = sessionRegistry.get("test-session-1").orElseThrow();
        chatSession.setAuthenticated(true);
        chatSession.setUserId(1L);

        when(chatRepository.findByChatIdAndUserUserId(12L, 1L)).thenReturn(Optional.empty());

        String messageJson = "{\"type\":\"SEND_MESSAGE\",\"chatId\":12,\"content\":\"Hello\",\"contentType\":\"TEXT\"}";
        chatWebSocketHandler.handleTextMessage(webSocketSession, new TextMessage(messageJson));

        ArgumentCaptor<Map<String, Object>> responseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webSocketSender, atLeastOnce()).send(eq(webSocketSession), responseCaptor.capture());

        Map<String, Object> response = responseCaptor.getValue();
        assertEquals(WebSocketMessageType.ERROR.getValue(), response.get("type"));
        assertEquals(WebSocketErrorCode.FORBIDDEN.getCode(), response.get("code"));
    }

    @Test
    void sendMessage_unauthenticated_rejectsWithMessage() throws Exception {
        chatWebSocketHandler.afterConnectionEstablished(webSocketSession);

        String messageJson = "{\"type\":\"SEND_MESSAGE\",\"chatId\":12,\"content\":\"Hello\",\"contentType\":\"TEXT\"}";
        chatWebSocketHandler.handleTextMessage(webSocketSession, new TextMessage(messageJson));

        ArgumentCaptor<Map<String, Object>> responseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webSocketSender, atLeastOnce()).send(eq(webSocketSession), responseCaptor.capture());

        Map<String, Object> response = responseCaptor.getValue();
        assertEquals(WebSocketMessageType.ERROR.getValue(), response.get("type"));
        assertEquals(WebSocketErrorCode.UNAUTHORIZED.getCode(), response.get("code"));
    }

    @Test
    void connectionClosed_unregistersSession() throws Exception {
        chatWebSocketHandler.afterConnectionEstablished(webSocketSession);

        assertTrue(sessionRegistry.get("test-session-1").isPresent());

        chatWebSocketHandler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL);

        assertFalse(sessionRegistry.get("test-session-1").isPresent());
    }
}
