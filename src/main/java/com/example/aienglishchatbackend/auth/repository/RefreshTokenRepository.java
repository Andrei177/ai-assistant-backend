package com.example.aienglishchatbackend.auth.repository;

import com.example.aienglishchatbackend.auth.entity.RefreshToken;
import com.example.aienglishchatbackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUserAndDeviceInfo(User user, String deviceInfo);
}
