package com.personal_ai.backend.service.provider.impl;

import com.personal_ai.backend.model.Provider;
import com.personal_ai.backend.repository.ProviderRepository;
import com.personal_ai.backend.service.provider.ProviderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ProviderServiceImpl implements ProviderService {

    private final ProviderRepository providerRepository;

    public ProviderServiceImpl(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @Override
    public Provider createProvider(String code, String displayName, Provider.ProviderType providerType, String baseUrl) {
        if (providerRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Provider code already exists: " + code);
        }

        Provider provider = Provider.builder()
                .code(code.trim().toLowerCase())
                .displayName(displayName.trim())
                .providerType(providerType)
                .baseUrl(baseUrl)
                .build();

        return providerRepository.save(provider);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Provider> findById(UUID id) {
        return providerRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Provider> findByCode(String code) {
        return providerRepository.findByCode(code.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Provider> findAll() {
        return providerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Provider> findActiveProviders() {
        return providerRepository.findByIsActiveTrue();
    }

    @Override
    public Provider updateProvider(UUID id, String displayName, String baseUrl, Boolean isActive) {
        Provider provider = providerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + id));

        if (displayName != null) {
            provider.setDisplayName(displayName.trim());
        }
        if (baseUrl != null) {
            provider.setBaseUrl(baseUrl.trim().isBlank() ? null : baseUrl.trim());
        }
        if (isActive != null) {
            provider.setIsActive(isActive);
        }

        return providerRepository.save(provider);
    }

    @Override
    public void deleteProvider(UUID id) {
        if (!providerRepository.existsById(id)) {
            throw new IllegalArgumentException("Provider not found: " + id);
        }
        providerRepository.deleteById(id);
    }
}
