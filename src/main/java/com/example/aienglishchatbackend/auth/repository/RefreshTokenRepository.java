package com.example.aienglishchatbackend.auth.repository;

import com.example.aienglishchatbackend.auth.entity.RefreshToken;
import com.example.aienglishchatbackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUserAndDeviceInfo(User user, String deviceInfo);

    List<RefreshToken> findAllByUserAndIsRevokedFalse(User user);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.user = :user AND rt.isRevoked = false")
    int revokeAllByUser(@Param("user") User user);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.tokenHash = :tokenHash")
    int revokeByTokenHash(@Param("tokenHash") String tokenHash);
}
