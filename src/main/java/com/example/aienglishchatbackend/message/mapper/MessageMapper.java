package com.example.aienglishchatbackend.message.mapper;

import com.example.aienglishchatbackend.message.dto.res.MessageResponse;
import com.example.aienglishchatbackend.message.entity.Message;

public class MessageMapper {
    public static MessageResponse messageToMessageResponse(Message message) {
        return new MessageResponse(
            message.getMessageId(),
            message.getContent(),
            message.getContentType(),
            message.getRole(),
            message.getTranscription(),
            message.getCreatedAt()
        );
    }
}
