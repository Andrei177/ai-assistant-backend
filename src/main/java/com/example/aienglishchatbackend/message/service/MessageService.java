package com.example.aienglishchatbackend.message.service;

import com.example.aienglishchatbackend.chat.entity.Chat;
import com.example.aienglishchatbackend.chat.service.ChatService;
import com.example.aienglishchatbackend.common.exception.ResourceNotFound;
import com.example.aienglishchatbackend.common.utils.FileService;
import com.example.aienglishchatbackend.llm.dto.GrammarFeedback;
import com.example.aienglishchatbackend.llm.service.LlmService;
import com.example.aienglishchatbackend.message.dto.res.HandleVoiceMessageResponse;
import com.example.aienglishchatbackend.message.dto.res.TranslateResponse;
import com.example.aienglishchatbackend.message.entity.Message;
import com.example.aienglishchatbackend.message.entity.MessageFeedback;
import com.example.aienglishchatbackend.message.repository.MessageFeedbackRepository;
import com.example.aienglishchatbackend.message.repository.MessageRepository;
import com.example.aienglishchatbackend.transcription.service.TranscriptionService;
import com.example.aienglishchatbackend.user.entity.User;
import com.example.aienglishchatbackend.websocket.handler.ChatWebSocketHandler;
import com.example.aienglishchatbackend.websocket.model.WebSocketMessageType;
import com.example.aienglishchatbackend.websocket.session.ChatSession;
import com.example.aienglishchatbackend.websocket.session.ChatSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.TextMessage;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageFeedbackRepository messageFeedbackRepository;
    private final ChatService chatService;
    private final LlmService llmService;
    private final FileService fileService;
    private final TranscriptionService transcriptionService;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatSessionRegistry chatSessionRegistry;
    private final ObjectMapper objectMapper;

    public Page<Message> getMessagesByChatId(Long chatId, User user, Pageable pageable) {
        // Проверка прав: чат должен принадлежать пользователю
        chatService.getChatById(chatId, user);
        return messageRepository.findByChat_ChatId(chatId, pageable);
    }

    public Message getMessageById(Long messageId, User user) {
        Message message = messageRepository.findByMessageId(messageId)
                .orElseThrow(() -> new ResourceNotFound("Сообщение не найдено"));
        
        // Проверка прав: сообщение должно принадлежать чату, который принадлежит пользователю
        if (!message.getChat().getUser().getUserId().equals(user.getUserId())) {
            throw new ResourceNotFound("Сообщение не найдено");
        }
        
        return message;
    }

    public MessageFeedback getMessageFeedback(Long messageId, User user){
        Message message = getMessageById(messageId, user);

        Optional<MessageFeedback> candidateMsgFeedback = messageFeedbackRepository.findByMessage_MessageId(messageId);

        if(candidateMsgFeedback.isPresent()){
            return candidateMsgFeedback.get();
        }

        GrammarFeedback feedback = llmService.analyzeGrammar(message.getContent());

        MessageFeedback msgFeedback = MessageFeedback.builder()
                .correctedText(feedback.getCorrectedText())
                .explanation(feedback.getExplanation())
                .grammarScore(feedback.getGrammarScore())
                .naturalnessScore(feedback.getNaturalnessScore())
                .overallScore(feedback.getOverallScore())
                .vocabularyScore(feedback.getVocabularyScore())
                .message(message)
                .build();

        return messageFeedbackRepository.save(msgFeedback);
    }

    public TranslateResponse translateMessage(Long messageId, String targetLanguage, User user){
        Message message = getMessageById(messageId, user);

        String translated = llmService.translate(message.getContent(), targetLanguage);

        return new TranslateResponse(messageId, message.getContent(), translated, targetLanguage);
    }

    public HandleVoiceMessageResponse handleVoiceMessage(Long chatId, MultipartFile audioFile, User user) {
        File wavFile = null;
        try {
            String pathSavedFile = fileService.saveFile(audioFile);
            File savedFile = fileService.getFile(pathSavedFile);
            wavFile = fileService.convertToWav(savedFile);
            String transcription = transcriptionService.transcribe(wavFile);
            
            Chat chat = chatService.getChatById(chatId, user);
            
            Message message = new Message();
            message.setContent(transcription);
            message.setContentType(Message.ContentType.VOICE);
            message.setRole(Message.Role.USER);
            message.setTranscription(transcription);
            message.setAudioUrl(pathSavedFile);
            message.setChat(chat);
            message.setUser(user);
            messageRepository.save(message);
            
            List<ChatSession> sessions = chatSessionRegistry.findByUserId(user.getUserId());
            
            for (ChatSession session : sessions) {
                 Map<String, Object> payload = new LinkedHashMap<>();
                 payload.put("type", WebSocketMessageType.SEND_MESSAGE);
                 payload.put("chatId", chatId);
                 payload.put("content", transcription);
                 payload.put("contentType", "TEXT");
                 payload.put("messageId", message.getMessageId());
                 
                 String json = objectMapper.writeValueAsString(payload);
                 
                 try {
                    chatWebSocketHandler.handleTextMessage(session.getSession(), new TextMessage(json));
                 } catch (Exception e) {
                    log.error("Failed to send WS message to session {}", session.getSessionId(), e);
                 }
            }
            
            return new HandleVoiceMessageResponse(message.getMessageId(), transcription);
        } catch (Exception e) {
            log.error("Error handling voice message for chat {}", chatId, e);
            throw new RuntimeException("Failed to process voice message", e);
        } finally {
            if (wavFile != null && wavFile.exists()) {
                wavFile.delete();
            }
        }
    }
}
