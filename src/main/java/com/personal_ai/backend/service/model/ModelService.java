package com.personal_ai.backend.service.model;

import com.personal_ai.backend.model.Model;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModelService {

    Model createModel(UUID providerId, String modelName, String displayName, Integer contextWindow);
    Model createModel(UUID providerId, String modelName, String displayName, Integer contextWindow, Boolean isOffline);

    Optional<Model> findById(UUID id);

    List<Model> findByProviderId(UUID providerId);

    List<Model> findActiveModelsByProviderId(UUID providerId);

    List<Model> findAll();

    List<Model> findByModelType(Model.ModelType modelType);

    List<Model> findActiveByModelType(Model.ModelType modelType);

    Model updateModel(UUID id, String displayName, Integer contextWindow, Boolean isActive);

    void deleteModel(UUID id);

    long deleteAllModels();
}
