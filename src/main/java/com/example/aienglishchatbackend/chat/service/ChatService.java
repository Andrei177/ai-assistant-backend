package com.example.aienglishchatbackend.chat.service;

import com.example.aienglishchatbackend.chat.dto.req.CreateChatRequest;
import com.example.aienglishchatbackend.chat.dto.req.UpdateChatRequest;
import com.example.aienglishchatbackend.chat.entity.Chat;
import com.example.aienglishchatbackend.chat.repository.ChatRepository;
import com.example.aienglishchatbackend.common.exception.ResourceNotFound;
import com.example.aienglishchatbackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;

    public Chat createChat(CreateChatRequest request, User user) {
        Chat chatToSave = Chat.builder()
                .chatName(request.getChatName())
                .user(user)
                .build();
        return chatRepository.save(chatToSave);
    }

    public Page<Chat> getChats(User user, Pageable pageable) {
        return chatRepository.findByUser_UserId(user.getUserId(), pageable);
    }

    public Chat getChatById(Long chatId, User user) {
        return chatRepository.findByChatIdAndUserUserId(chatId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFound("Чат не найден"));
    }

    public Chat updateChat(Long chatId, UpdateChatRequest request, User user) {
        Chat chat = getChatById(chatId, user);
        chat.setChatName(request.getChatName());
        return chatRepository.save(chat);
    }

    public void deleteChat(Long chatId, User user) {
        Chat chat = getChatById(chatId, user);
        chatRepository.delete(chat);
    }
}
