package com.example.aienglishchatbackend.chat.controller;

import com.example.aienglishchatbackend.chat.dto.req.CreateChatRequest;
import com.example.aienglishchatbackend.chat.dto.req.UpdateChatRequest;
import com.example.aienglishchatbackend.chat.dto.res.ChatResponse;
import com.example.aienglishchatbackend.chat.dto.res.CreateChatResponse;
import com.example.aienglishchatbackend.chat.entity.Chat;
import com.example.aienglishchatbackend.chat.mapper.ChatMapper;
import com.example.aienglishchatbackend.chat.service.ChatService;
import com.example.aienglishchatbackend.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    
    @PostMapping
    public ResponseEntity<CreateChatResponse> createChat(@Valid @RequestBody CreateChatRequest request, @AuthenticationPrincipal User user){
        Chat createdChat = chatService.createChat(request, user);
        return ResponseEntity.ok(ChatMapper.chatToCreateChatResponse(createdChat));
    }

    @GetMapping
    public ResponseEntity<Page<ChatResponse>> getChats(@AuthenticationPrincipal User user, Pageable pageable) {
        Page<Chat> chats = chatService.getChats(user, pageable);
        return ResponseEntity.ok(chats.map(ChatMapper::chatToChatResponse));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChat(@PathVariable Long chatId, @AuthenticationPrincipal User user) {
        Chat chat = chatService.getChatById(chatId, user);
        return ResponseEntity.ok(ChatMapper.chatToChatResponse(chat));
    }

    @PatchMapping("/{chatId}")
    public ResponseEntity<ChatResponse> updateChat(@PathVariable Long chatId, @Valid @RequestBody UpdateChatRequest request, @AuthenticationPrincipal User user) {
        Chat chat = chatService.updateChat(chatId, request, user);
        return ResponseEntity.ok(ChatMapper.chatToChatResponse(chat));
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long chatId, @AuthenticationPrincipal User user) {
        chatService.deleteChat(chatId, user);
        return ResponseEntity.noContent().build();
    }
}
