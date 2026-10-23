package com.example.aienglishchatbackend.message.service;

import com.example.aienglishchatbackend.chat.service.ChatService;
import com.example.aienglishchatbackend.common.exception.ResourceNotFound;
import com.example.aienglishchatbackend.llm.dto.GrammarFeedback;
import com.example.aienglishchatbackend.llm.service.LlmService;
import com.example.aienglishchatbackend.message.entity.Message;
import com.example.aienglishchatbackend.message.entity.MessageFeedback;
import com.example.aienglishchatbackend.message.repository.MessageFeedbackRepository;
import com.example.aienglishchatbackend.message.repository.MessageRepository;
import com.example.aienglishchatbackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageFeedbackRepository messageFeedbackRepository;
    private final ChatService chatService;
    private final LlmService llmService;

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
}
