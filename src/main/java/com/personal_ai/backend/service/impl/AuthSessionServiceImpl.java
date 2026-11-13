package com.personal_ai.backend.service.impl;

import com.personal_ai.backend.model.AuthSession;
import com.personal_ai.backend.model.User;
import com.personal_ai.backend.repository.AuthSessionRepository;
import com.personal_ai.backend.repository.UserRepository;
import com.personal_ai.backend.service.AuthSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;

@Service
@Transactional
public class AuthSessionServiceImpl implements AuthSessionService {

    private final AuthSessionRepository authSessionRepository;
    private final UserRepository userRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public AuthSessionServiceImpl(AuthSessionRepository authSessionRepository, UserRepository userRepository) {
        this.authSessionRepository = authSessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public AuthSession createSession(UUID userId, byte[] refreshTokenHash, String userAgent, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        InetAddress addr = null;
        if (ipAddress != null) {
            try {
                addr = InetAddress.getByName(ipAddress);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid IP address: " + ipAddress);
            }
        }

        AuthSession session = AuthSession.builder()
                .user(user)
                .refreshTokenHash(refreshTokenHash)
                .expiresAt(OffsetDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)))
                .userAgent(userAgent)
                .ipAddress(addr)
                .build();

        return authSessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthSession> findById(UUID id) {
        return authSessionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthSession> findByRefreshTokenHash(byte[] refreshTokenHash) {
        return authSessionRepository.findByRefreshTokenHash(refreshTokenHash);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthSession> findActiveSessionsByUserId(UUID userId) {
        return authSessionRepository.findActiveSessionsByUserId(userId, OffsetDateTime.now());
    }

    @Override
    public void revokeSession(UUID id) {
        AuthSession session = authSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + id));

        session.setRevokedAt(OffsetDateTime.now());
        authSessionRepository.save(session);
    }

    @Override
    public void revokeAllSessionsByUserId(UUID userId) {
        List<AuthSession> sessions = authSessionRepository.findActiveSessionsByUserId(userId, OffsetDateTime.now());
        OffsetDateTime now = OffsetDateTime.now();
        sessions.forEach(session -> session.setRevokedAt(now));
        authSessionRepository.saveAll(sessions);
    }

    @Override
    public AuthSession rotateSession(UUID sessionId) {
        AuthSession oldSession = authSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (oldSession.getRevokedAt() != null) {
            throw new IllegalStateException("Session has been revoked: " + sessionId);
        }

        OffsetDateTime now = OffsetDateTime.now();
        oldSession.setLastUsedAt(now);
        authSessionRepository.save(oldSession);

        return oldSession;
    }
}
