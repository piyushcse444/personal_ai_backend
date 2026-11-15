package com.personal_ai.backend.service.provider;

import com.personal_ai.backend.model.ProviderConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderConfigService {

    ProviderConfig createConfig(UUID userId, UUID providerId, String configName, byte[] credentialCiphertext, String credentialKeyVersion, String endpointUrl);

    Optional<ProviderConfig> findById(UUID id);

    Optional<ProviderConfig> findByUserIdAndId(UUID userId, UUID id);

    List<ProviderConfig> findByUserId(UUID userId);

    List<ProviderConfig> findByUserIdAndProviderId(UUID userId, UUID providerId);

    List<ProviderConfig> findActiveConfigsByUserId(UUID userId);

    ProviderConfig updateConfig(UUID id, String configName, byte[] credentialCiphertext, String credentialKeyVersion, String endpointUrl, Boolean isActive);

    void deleteConfig(UUID id);
}
