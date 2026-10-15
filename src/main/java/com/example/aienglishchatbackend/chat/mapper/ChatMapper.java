package com.example.aienglishchatbackend.chat.mapper;

import com.example.aienglishchatbackend.chat.dto.res.ChatResponse;
import com.example.aienglishchatbackend.chat.dto.res.CreateChatResponse;
import com.example.aienglishchatbackend.chat.entity.Chat;

public class ChatMapper {
    public static CreateChatResponse chatToCreateChatResponse(Chat chat) {
        return new CreateChatResponse(chat.getChatId(), chat.getChatName(), chat.getCreatedAt());
    }

    public static ChatResponse chatToChatResponse(Chat chat) {
        return new ChatResponse(chat.getChatId(), chat.getChatName(), chat.getCreatedAt(), chat.getUpdatedAt());
    }
}
