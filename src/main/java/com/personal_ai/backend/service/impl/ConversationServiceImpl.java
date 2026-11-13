package com.personal_ai.backend.service.impl;

import com.personal_ai.backend.model.Conversation;
import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.model.Provider;
import com.personal_ai.backend.model.ProviderConfig;
import com.personal_ai.backend.model.User;
import com.personal_ai.backend.repository.ConversationRepository;
import com.personal_ai.backend.repository.ModelRepository;
import com.personal_ai.backend.repository.ProviderConfigRepository;
import com.personal_ai.backend.repository.ProviderRepository;
import com.personal_ai.backend.repository.UserRepository;
import com.personal_ai.backend.service.ConversationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final ModelRepository modelRepository;
    private final ProviderConfigRepository providerConfigRepository;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   UserRepository userRepository,
                                   ProviderRepository providerRepository,
                                   ModelRepository modelRepository,
                                   ProviderConfigRepository providerConfigRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.providerConfigRepository = providerConfigRepository;
    }

    @Override
    public Conversation createConversation(UUID userId, UUID providerId, UUID modelId, UUID providerConfigId, String title) {
        // Use lightweight checks instead of loading full entities with all their relationships
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        if (!providerRepository.existsById(providerId)) {
            throw new IllegalArgumentException("Provider not found: " + providerId);
        }

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        if (!provider.getIsActive()) {
            throw new IllegalStateException("Provider is not active: " + providerId);
        }

        if (!modelRepository.existsById(modelId)) {
            throw new IllegalArgumentException("Model not found: " + modelId);
        }

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        if (!model.getProvider().getId().equals(providerId)) {
            throw new IllegalArgumentException("Model does not belong to the specified provider");
        }

        if (!model.getIsActive()) {
            throw new IllegalStateException("Model is not active: " + modelId);
        }

        if (!providerConfigRepository.existsById(providerConfigId)) {
            throw new IllegalArgumentException("Provider config not found: " + providerConfigId);
        }

        ProviderConfig config = providerConfigRepository.findByUserIdAndId(userId, providerConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Provider config does not belong to the user"));

        if (!config.getProvider().getId().equals(providerId)) {
            throw new IllegalArgumentException("Provider config does not belong to the specified provider");
        }

        if (!config.getIsActive()) {
            throw new IllegalStateException("Provider config is not active: " + providerConfigId);
        }

        // Use lightweight entity references instead of full entities
        User user = new User();
        user.setId(userId);

        Conversation conversation = Conversation.builder()
                .user(user)
                .provider(provider)
                .model(model)
                .providerConfig(config)
                .title((title != null && !title.isBlank()) ? title.trim() : "New conversation")
                .build();

        return conversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findById(UUID id) {
        return conversationRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findByUserIdAndId(UUID userId, UUID id) {
        return conversationRepository.findByUserIdAndId(userId, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> findByUserId(UUID userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> findRecentConversationsByUserId(UUID userId, int limit) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .limit(limit)
                .toList();
    }

    @Override
    public Conversation updateConversation(UUID id, String title, UUID providerId, UUID modelId, UUID providerConfigId) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + id));

        if (title != null) {
            conversation.setTitle(title.trim().isBlank() ? "New conversation" : title.trim());
        }

        if (providerId != null && modelId != null && providerConfigId != null) {
            Provider provider = providerRepository.findById(providerId)
                    .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

            Model model = modelRepository.findById(modelId)
                    .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

            if (!model.getProvider().getId().equals(providerId)) {
                throw new IllegalArgumentException("Model does not belong to the specified provider");
            }

            ProviderConfig config = providerConfigRepository.findByUserIdAndId(
                    conversation.getUser().getId(), providerConfigId)
                    .orElseThrow(() -> new IllegalArgumentException("Provider config does not belong to the conversation's user"));

            if (!config.getProvider().getId().equals(providerId)) {
                throw new IllegalArgumentException("Provider config does not belong to the specified provider");
            }

            conversation.setProvider(provider);
            conversation.setModel(model);
            conversation.setProviderConfig(config);
        }

        return conversationRepository.save(conversation);
    }

    @Override
    public void deleteConversation(UUID id) {
        if (!conversationRepository.existsById(id)) {
            throw new IllegalArgumentException("Conversation not found: " + id);
        }
        conversationRepository.deleteById(id);
    }
}
