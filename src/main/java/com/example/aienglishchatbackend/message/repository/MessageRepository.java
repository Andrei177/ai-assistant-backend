package com.example.aienglishchatbackend.message.repository;

import com.example.aienglishchatbackend.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByChat_ChatId(Long chatId, Pageable pageable);
    
    Optional<Message> findByMessageId(Long messageId);
}
