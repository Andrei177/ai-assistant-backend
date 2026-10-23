package com.example.aienglishchatbackend.message.controller;

import com.example.aienglishchatbackend.message.dto.res.MessageFeedbackResponse;
import com.example.aienglishchatbackend.message.dto.res.MessageResponse;
import com.example.aienglishchatbackend.message.entity.Message;
import com.example.aienglishchatbackend.message.entity.MessageFeedback;
import com.example.aienglishchatbackend.message.mapper.MessageMapper;
import com.example.aienglishchatbackend.message.service.MessageService;
import com.example.aienglishchatbackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/chats/{chatId}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessagesByChatId(
            @PathVariable Long chatId,
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        Page<Message> messages = messageService.getMessagesByChatId(chatId, user, pageable);
        return ResponseEntity.ok(messages.map(MessageMapper::messageToMessageResponse));
    }

    @GetMapping("/messages/{messageId}")
    public ResponseEntity<MessageResponse> getMessageById(
            @PathVariable Long messageId,
            @AuthenticationPrincipal User user) {
        Message message = messageService.getMessageById(messageId, user);
        return ResponseEntity.ok(MessageMapper.messageToMessageResponse(message));
    }

    @GetMapping("/messages/{messageId}/feedback")
    public ResponseEntity<MessageFeedbackResponse> getMessageFeedback(@PathVariable Long messageId, @AuthenticationPrincipal User user){
        MessageFeedback feedback = messageService.getMessageFeedback(messageId, user);

        return ResponseEntity.ok(
                MessageMapper.messageFeedbackToMessageFeedbackResponse(feedback)
        );
    }
}
