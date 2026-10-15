package com.example.aienglishchatbackend.chat.repository;

import com.example.aienglishchatbackend.chat.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    Page<Chat> findByUser_UserId(Long userId, Pageable pageable);
    Optional<Chat> findByChatIdAndUser_UserId(Long chatId, Long userId);
}
