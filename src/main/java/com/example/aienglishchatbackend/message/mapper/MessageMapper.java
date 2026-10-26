package com.example.aienglishchatbackend.message.mapper;

import com.example.aienglishchatbackend.message.dto.res.MessageFeedbackResponse;
import com.example.aienglishchatbackend.message.dto.res.MessageResponse;
import com.example.aienglishchatbackend.message.entity.Message;
import com.example.aienglishchatbackend.message.entity.MessageFeedback;

public class MessageMapper {
    public static MessageResponse messageToMessageResponse(Message message) {
        return new MessageResponse(
            message.getMessageId(),
            message.getContent(),
            message.getContentType(), message.getAudioUrl(),
            message.getRole(),
            message.getTranscription(),
            message.getCreatedAt()
        );
    }

    public static MessageFeedbackResponse messageFeedbackToMessageFeedbackResponse(MessageFeedback feedback){
        return new MessageFeedbackResponse(
                feedback.getMessage().getMessageId(),
                feedback.getOverallScore(),
                feedback.getGrammarScore(),
                feedback.getVocabularyScore(),
                feedback.getNaturalnessScore(),
                feedback.getCorrectedText(),
                feedback.getExplanation()
        );
    }
}
