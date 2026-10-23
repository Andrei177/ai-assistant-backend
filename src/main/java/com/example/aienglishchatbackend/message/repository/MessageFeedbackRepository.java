package com.example.aienglishchatbackend.message.repository;

import com.example.aienglishchatbackend.message.entity.MessageFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageFeedbackRepository extends JpaRepository<MessageFeedback, Long> {
    Optional<MessageFeedback> findByMessage_MessageId(Long messageId);
}
