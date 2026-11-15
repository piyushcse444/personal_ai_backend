package com.personal_ai.backend.service.provider;

import com.personal_ai.backend.model.Provider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderService {

    Provider createProvider(String code, String displayName, Provider.ProviderType providerType, String baseUrl);

    Optional<Provider> findById(UUID id);

    Optional<Provider> findByCode(String code);

    List<Provider> findAll();

    List<Provider> findActiveProviders();

    Provider updateProvider(UUID id, String displayName, String baseUrl, Boolean isActive);

    void deleteProvider(UUID id);
}
