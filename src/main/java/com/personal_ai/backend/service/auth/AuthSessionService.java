package com.personal_ai.backend.service.auth;

import com.personal_ai.backend.model.AuthSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionService {

    AuthSession createSession(UUID userId, byte[] refreshTokenHash, String userAgent, String ipAddress);

    Optional<AuthSession> findById(UUID id);

    Optional<AuthSession> findByRefreshTokenHash(byte[] refreshTokenHash);

    List<AuthSession> findActiveSessionsByUserId(UUID userId);

    void revokeSession(UUID id);

    void revokeAllSessionsByUserId(UUID userId);

    AuthSession rotateSession(UUID sessionId);
}
