package com.personal_ai.backend.service.conversation.impl;

import com.personal_ai.backend.model.Conversation;
import com.personal_ai.backend.model.Message;
import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.repository.ConversationRepository;
import com.personal_ai.backend.repository.MessageRepository;
import com.personal_ai.backend.repository.ModelRepository;
import com.personal_ai.backend.service.conversation.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ModelRepository modelRepository;

    public MessageServiceImpl(MessageRepository messageRepository,
                              ConversationRepository conversationRepository,
                              ModelRepository modelRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.modelRepository = modelRepository;
    }

    @Override
    public Message createMessage(UUID conversationId, String role, String content, Integer tokenCount, UUID modelId) {
        Message.Role messageRole;
        try {
            messageRole = Message.Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role + ". Must be one of: SYSTEM, USER, ASSISTANT");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        if (tokenCount != null && tokenCount < 0) {
            throw new IllegalArgumentException("Token count cannot be negative");
        }

        // Use lightweight entity references instead of loading full entities.
        // Let the database foreign key constraint validate existence — this avoids race conditions
        // where the conversation/model is deleted between the existsById check and the save().
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);

        Model model = null;
        if (modelId != null) {
            model = new Model();
            model.setId(modelId);
        }

        Message message = Message.builder()
                .conversation(conversation)
                .role(messageRole)
                .content(content)
                .tokenCount(tokenCount)
                .model(model)
                .build();

        return messageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Message> findById(UUID id) {
        return messageRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> findByConversationId(UUID conversationId) {
        return messageRepository.findByConversationId(conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Override
    public void deleteMessage(UUID id) {
        if (!messageRepository.existsById(id)) {
            throw new IllegalArgumentException("Message not found: " + id);
        }
        messageRepository.deleteById(id);
    }

    @Override
    public void deleteByConversationId(UUID conversationId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new IllegalArgumentException("Conversation not found: " + conversationId);
        }
        messageRepository.deleteByConversationId(conversationId);
    }
}
