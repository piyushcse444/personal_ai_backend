package com.personal_ai.backend.service.impl;

import com.personal_ai.backend.model.Provider;
import com.personal_ai.backend.model.ProviderConfig;
import com.personal_ai.backend.model.User;
import com.personal_ai.backend.repository.ProviderConfigRepository;
import com.personal_ai.backend.repository.ProviderRepository;
import com.personal_ai.backend.repository.UserRepository;
import com.personal_ai.backend.service.ProviderConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ProviderConfigServiceImpl implements ProviderConfigService {

    private final ProviderConfigRepository providerConfigRepository;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;

    public ProviderConfigServiceImpl(ProviderConfigRepository providerConfigRepository,
                                     UserRepository userRepository,
                                     ProviderRepository providerRepository) {
        this.providerConfigRepository = providerConfigRepository;
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
    }

    @Override
    public ProviderConfig createConfig(UUID userId, UUID providerId, String configName,
                                       byte[] credentialCiphertext, String credentialKeyVersion, String endpointUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        // Validate credential pair consistency
        if ((credentialCiphertext == null && credentialKeyVersion != null) ||
            (credentialCiphertext != null && credentialKeyVersion == null)) {
            throw new IllegalArgumentException("Credential ciphertext and key version must both be present or both be null");
        }

        ProviderConfig config = ProviderConfig.builder()
                .user(user)
                .provider(provider)
                .configName(configName.trim())
                .credentialCiphertext(credentialCiphertext)
                .credentialKeyVersion(credentialKeyVersion)
                .endpointUrl(endpointUrl)
                .build();

        return providerConfigRepository.save(config);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProviderConfig> findById(UUID id) {
        return providerConfigRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProviderConfig> findByUserIdAndId(UUID userId, UUID id) {
        return providerConfigRepository.findByUserIdAndId(userId, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderConfig> findByUserId(UUID userId) {
        return providerConfigRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderConfig> findByUserIdAndProviderId(UUID userId, UUID providerId) {
        return providerConfigRepository.findByUserIdAndProviderIdAndIsActiveTrue(userId, providerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderConfig> findActiveConfigsByUserId(UUID userId) {
        return providerConfigRepository.findByUserIdAndIsActiveTrue(userId);
    }

    @Override
    public ProviderConfig updateConfig(UUID id, String configName,
                                       byte[] credentialCiphertext, String credentialKeyVersion,
                                       String endpointUrl, Boolean isActive) {
        ProviderConfig config = providerConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Provider config not found: " + id));

        // Validate credential pair consistency on update
        byte[] newCiphertext = (credentialCiphertext != null) ? credentialCiphertext : config.getCredentialCiphertext();
        String newKeyVersion = (credentialKeyVersion != null) ? credentialKeyVersion : config.getCredentialKeyVersion();

        if ((newCiphertext == null && newKeyVersion != null) ||
            (newCiphertext != null && newKeyVersion == null)) {
            throw new IllegalArgumentException("Credential ciphertext and key version must both be present or both be null");
        }

        if (configName != null) {
            config.setConfigName(configName.trim());
        }
        config.setCredentialCiphertext(newCiphertext);
        config.setCredentialKeyVersion(newKeyVersion);
        if (endpointUrl != null) {
            config.setEndpointUrl(endpointUrl.trim().isBlank() ? null : endpointUrl.trim());
        }
        if (isActive != null) {
            config.setIsActive(isActive);
        }

        return providerConfigRepository.save(config);
    }

    @Override
    public void deleteConfig(UUID id) {
        if (!providerConfigRepository.existsById(id)) {
            throw new IllegalArgumentException("Provider config not found: " + id);
        }
        providerConfigRepository.deleteById(id);
    }
}
