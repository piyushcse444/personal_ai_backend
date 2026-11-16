package com.personal_ai.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "models")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Model {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    @JsonIgnore
    private Provider provider;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "context_window", nullable = false)
    private Integer contextWindow;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_offline", nullable = false)
    @Builder.Default
    private Boolean isOffline = false;

    @Column(name = "supports_vision", nullable = false)
    @Builder.Default
    private Boolean supportsVision = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false)
    @Builder.Default
    private ModelType modelType = ModelType.CHAT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "model")
    @JsonIgnore
    @Builder.Default
    private List<Conversation> conversations = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "model")
    @JsonIgnore
    @Builder.Default
    private List<Message> messages = new java.util.ArrayList<>();

    public enum ModelType {
        CHAT,
        ASR,
        TTS,
        EMBEDDING
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
