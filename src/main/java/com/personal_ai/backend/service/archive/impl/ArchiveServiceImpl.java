package com.personal_ai.backend.service.archive.impl;

import com.personal_ai.backend.model.ArchiveConversation;
import com.personal_ai.backend.model.ArchiveMessage;
import com.personal_ai.backend.model.Conversation;
import com.personal_ai.backend.model.Message;
import com.personal_ai.backend.repository.ArchiveConversationRepository;
import com.personal_ai.backend.repository.ArchiveMessageRepository;
import com.personal_ai.backend.repository.ConversationRepository;
import com.personal_ai.backend.repository.MessageRepository;
import com.personal_ai.backend.service.archive.ArchiveService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ArchiveServiceImpl implements ArchiveService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ArchiveConversationRepository archiveConversationRepository;
    private final ArchiveMessageRepository archiveMessageRepository;

    public ArchiveServiceImpl(ConversationRepository conversationRepository,
                              MessageRepository messageRepository,
                              ArchiveConversationRepository archiveConversationRepository,
                              ArchiveMessageRepository archiveMessageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.archiveConversationRepository = archiveConversationRepository;
        this.archiveMessageRepository = archiveMessageRepository;
    }

    @Override
    public ArchiveConversation archiveConversation(UUID conversationId) {
        // Fetch the conversation with all its relationships
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        // Extract string values from relationships before they are deleted
        String userEmail = conversation.getUser().getEmail();
        String providerName = conversation.getProvider().getDisplayName();
        String modelName = conversation.getModel().getModelName();
        String providerConfigName = conversation.getProviderConfig() != null
                ? conversation.getProviderConfig().getConfigName()
                : null;

        // Build the archived conversation
        ArchiveConversation archived = ArchiveConversation.builder()
                .userEmail(userEmail)
                .providerName(providerName)
                .modelName(modelName)
                .title(conversation.getTitle())
                .providerConfigName(providerConfigName)
                .archivedAt(OffsetDateTime.now())
                .build();

        archived = archiveConversationRepository.save(archived);

        // Fetch and archive all messages
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        for (Message message : messages) {
            String msgModelName = message.getModel() != null ? message.getModel().getModelName() : null;

            ArchiveMessage archivedMessage = ArchiveMessage.builder()
                    .archiveConversation(archived)
                    .role(ArchiveMessage.Role.valueOf(message.getRole().name()))
                    .content(message.getContent())
                    .tokenCount(message.getTokenCount())
                    .modelName(msgModelName)
                    .archivedAt(OffsetDateTime.now())
                    .build();

            archiveMessageRepository.save(archivedMessage);
        }

        return archived;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArchiveConversation> getArchivedConversationsByUserEmail(String userEmail) {
        return archiveConversationRepository.findByUserEmailOrderByArchivedAtDesc(userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArchiveConversation> getArchivedConversationById(UUID archiveConversationId) {
        return archiveConversationRepository.findById(archiveConversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArchiveMessage> getArchivedMessages(UUID archiveConversationId) {
        if (!archiveConversationRepository.existsById(archiveConversationId)) {
            throw new IllegalArgumentException("Archived conversation not found: " + archiveConversationId);
        }
        return archiveMessageRepository.findByArchiveConversationIdOrderByArchivedAtAsc(archiveConversationId);
    }

    @Override
    public void deleteArchivedConversation(UUID archiveConversationId) {
        if (!archiveConversationRepository.existsById(archiveConversationId)) {
            throw new IllegalArgumentException("Archived conversation not found: " + archiveConversationId);
        }
        archiveConversationRepository.deleteById(archiveConversationId);
    }
}
