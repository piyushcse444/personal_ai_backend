package com.personal_ai.backend.service.impl;

import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.model.Provider;
import com.personal_ai.backend.repository.ModelRepository;
import com.personal_ai.backend.repository.ProviderRepository;
import com.personal_ai.backend.service.ModelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ModelServiceImpl implements ModelService {

    private final ModelRepository modelRepository;
    private final ProviderRepository providerRepository;

    public ModelServiceImpl(ModelRepository modelRepository, ProviderRepository providerRepository) {
        this.modelRepository = modelRepository;
        this.providerRepository = providerRepository;
    }

    @Override
    public Model createModel(UUID providerId, String modelName, String displayName, Integer contextWindow) {
        return createModel(providerId, modelName, displayName, contextWindow, false);
    }

    @Override
    public Model createModel(UUID providerId, String modelName, String displayName, Integer contextWindow, Boolean isOffline) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        if (contextWindow <= 0) {
            throw new IllegalArgumentException("Context window must be positive");
        }

        Model model = Model.builder()
                .provider(provider)
                .modelName(modelName.trim())
                .displayName(displayName.trim())
                .contextWindow(contextWindow)
                .isOffline(isOffline)
                .build();

        return modelRepository.save(model);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Model> findById(UUID id) {
        return modelRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Model> findByProviderId(UUID providerId) {
        return modelRepository.findByProviderId(providerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Model> findActiveModelsByProviderId(UUID providerId) {
        return modelRepository.findByProviderIdAndIsActiveTrue(providerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Model> findAll() {
        return modelRepository.findAll();
    }

    @Override
    public Model updateModel(UUID id, String displayName, Integer contextWindow, Boolean isActive) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));

        if (displayName != null) {
            model.setDisplayName(displayName.trim());
        }
        if (contextWindow != null && contextWindow > 0) {
            model.setContextWindow(contextWindow);
        }
        if (isActive != null) {
            model.setIsActive(isActive);
        }

        return modelRepository.save(model);
    }

    @Override
    public void deleteModel(UUID id) {
        if (!modelRepository.existsById(id)) {
            throw new IllegalArgumentException("Model not found: " + id);
        }
        modelRepository.deleteById(id);
    }

    @Override
    public long deleteAllModels() {
        long count = modelRepository.count();
        modelRepository.deleteAll();
        return count;
    }
}
