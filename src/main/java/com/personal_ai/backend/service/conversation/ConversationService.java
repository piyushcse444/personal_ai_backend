package com.personal_ai.backend.service.conversation;

import com.personal_ai.backend.model.Conversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationService {

    Conversation createConversation(UUID userId, UUID providerId, UUID modelId, UUID providerConfigId, String title);
    Conversation createConversation(UUID userId, UUID providerId, UUID modelId, String title);

    Optional<Conversation> findById(UUID id);

    Optional<Conversation> findByUserIdAndId(UUID userId, UUID id);

    List<Conversation> findByUserId(UUID userId);

    List<Conversation> findRecentConversationsByUserId(UUID userId, int limit);

    Conversation updateConversation(UUID id, String title, UUID providerId, UUID modelId, UUID providerConfigId);

    void deleteConversation(UUID id);
}
