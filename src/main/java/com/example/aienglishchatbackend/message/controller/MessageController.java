package com.example.aienglishchatbackend.message.controller;

import com.example.aienglishchatbackend.message.dto.req.TranslateRequest;
import com.example.aienglishchatbackend.message.dto.res.HandleVoiceMessageResponse;
import com.example.aienglishchatbackend.message.dto.res.MessageFeedbackResponse;
import com.example.aienglishchatbackend.message.dto.res.MessageResponse;
import com.example.aienglishchatbackend.message.dto.res.TranslateResponse;
import com.example.aienglishchatbackend.message.entity.Message;
import com.example.aienglishchatbackend.message.entity.MessageFeedback;
import com.example.aienglishchatbackend.message.mapper.MessageMapper;
import com.example.aienglishchatbackend.message.service.MessageService;
import com.example.aienglishchatbackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    @Value("${file.store.path}")
    private String STORE_DIRECTORY;

    @GetMapping("/chats/{chatId}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessagesByChatId(
            @PathVariable Long chatId,
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        Page<Message> messages = messageService.getMessagesByChatId(chatId, user, pageable);
        return ResponseEntity.ok(messages.map(message -> MessageMapper.messageToMessageResponse(message, chatId)));
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

    @PostMapping("/messages/{messageId}/translate")
    public ResponseEntity<TranslateResponse> translateMessage(@PathVariable Long messageId, @RequestBody TranslateRequest req, @AuthenticationPrincipal User user){
        return ResponseEntity.ok(
                messageService.translateMessage(messageId, req.targetLanguage(), user)
        );
    }

    @PostMapping("messages/voice")
    public ResponseEntity<HandleVoiceMessageResponse> handleVoiceMessage(@RequestParam("chatId") Long chatId, @RequestParam("audio") MultipartFile audioFile, @AuthenticationPrincipal User user){
        return ResponseEntity.ok(messageService.handleVoiceMessage(chatId, audioFile, user));
    }

    @GetMapping("/messages/{messageId}/audio")
    public ResponseEntity<ResourceRegion> getAudio(
            @PathVariable Long messageId,
            @RequestHeader HttpHeaders headers,
            @AuthenticationPrincipal User user) {

        Message message = messageService.getMessageById(messageId, user);
        if (message.getAudioUrl() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            // 1. Соединяем путь к базовой папке и имя файла из БД безопасным образом
            Path rootPath = Paths.get(STORE_DIRECTORY).toAbsolutePath().normalize();
            Path filePath = rootPath.resolve(message.getAudioUrl()).toAbsolutePath().normalize();

            // 2. Проверяем безопасность: файл должен физически находиться внутри нашей STORE_DIRECTORY
            if (!filePath.startsWith(rootPath)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "audio/mpeg";
            }

            ResourceRegion region = resourceRegion(resource, headers);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT) // 206 Partial Content
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName().toString() + "\"")
                    .body(region);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Утилитный метод, который определяет, сколько байт за раз отдавать браузеру (например, по 1 MB)
    private ResourceRegion resourceRegion(Resource resource, HttpHeaders headers) throws IOException {
        long contentLength = resource.contentLength();
        HttpRange range = headers.getRange().isEmpty() ? null : headers.getRange().get(0);

        if (range != null) {
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(1024 * 1024L, end - start + 1); // Читаем чанками по 1 МБ
            return new ResourceRegion(resource, start, rangeLength);
        } else {
            long rangeLength = Math.min(1024 * 1024L, contentLength);
            return new ResourceRegion(resource, 0, rangeLength);
        }
    }
}
