package com.personal_ai.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "archive_message")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_conversation_id", nullable = false)
    private ArchiveConversation archiveConversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "archived_at", nullable = false, updatable = false)
    private OffsetDateTime archivedAt;

    @PrePersist
    protected void onCreate() {
        if (archivedAt == null) {
            archivedAt = OffsetDateTime.now();
        }
    }

    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT
    }
}
