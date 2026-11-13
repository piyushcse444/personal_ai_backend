package com.personal_ai.backend.repository;

import com.personal_ai.backend.model.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    Optional<AuthSession> findByRefreshTokenHash(byte[] refreshTokenHash);

    @Query("SELECT a FROM AuthSession a WHERE a.user.id = :userId AND a.revokedAt IS NULL AND a.expiresAt > :now")
    List<AuthSession> findActiveSessionsByUserId(UUID userId, OffsetDateTime now);

    @Query("SELECT a FROM AuthSession a WHERE a.user.id = :userId AND a.revokedAt IS NOT NULL")
    List<AuthSession> findRevokedSessionsByUserId(UUID userId);

    @Query("SELECT a FROM AuthSession a WHERE a.expiresAt < :now AND a.revokedAt IS NULL")
    List<AuthSession> findExpiredSessions(OffsetDateTime now);
}
