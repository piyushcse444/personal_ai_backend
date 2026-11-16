package com.personal_ai.backend.service.conversation;

import com.personal_ai.backend.model.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageService {

    Message createMessage(UUID conversationId, String role, String content, Integer tokenCount, UUID modelId);
    Message createMessage(UUID conversationId, String role, String content, String imageBase64, String imageMimeType, UUID modelId);

    Optional<Message> findById(UUID id);

    List<Message> findByConversationId(UUID conversationId);

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    void deleteMessage(UUID id);

    void deleteByConversationId(UUID conversationId);
}
