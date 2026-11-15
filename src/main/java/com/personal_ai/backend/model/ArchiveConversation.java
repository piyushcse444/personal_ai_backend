package com.personal_ai.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "archive_conversation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(nullable = false)
    private String title;

    @Column(name = "provider_config_name")
    private String providerConfigName;

    @Column(name = "archived_at", nullable = false, updatable = false)
    private OffsetDateTime archivedAt;

    @OneToMany(mappedBy = "archiveConversation", cascade = CascadeType.REMOVE)
    @Builder.Default
    private List<ArchiveMessage> messages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (archivedAt == null) {
            archivedAt = OffsetDateTime.now();
        }
    }
}
