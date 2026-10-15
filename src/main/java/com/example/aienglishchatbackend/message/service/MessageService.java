package com.example.aienglishchatbackend.message.service;

import com.example.aienglishchatbackend.chat.service.ChatService;
import com.example.aienglishchatbackend.common.exception.ResourceNotFound;
import com.example.aienglishchatbackend.message.entity.Message;
import com.example.aienglishchatbackend.message.repository.MessageRepository;
import com.example.aienglishchatbackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final ChatService chatService;

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
}
