package com.personal_ai.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, columnDefinition = "bytea")
    private byte[] refreshTokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_id")
    private AuthSession replacedBy;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(columnDefinition = "inet")
    private InetAddress ipAddress;

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = OffsetDateTime.now();
        }
    }
}
