package com.personal_ai.backend.controller;

import com.personal_ai.backend.dto.ModelResponse;
import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.service.model.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/models")
@Tag(name = "Models", description = "Model listing and discovery APIs")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping
    @Operation(summary = "List models", description = "List models, optionally filtered by model type (CHAT, ASR, TTS, EMBEDDING) or provider ID")
    public ResponseEntity<?> listModels(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID providerId) {
        try {
            List<Model> models;
            if (type != null && !type.isEmpty()) {
                try {
                    Model.ModelType modelType = Model.ModelType.valueOf(type.toUpperCase());
                    models = modelService.findActiveByModelType(modelType);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Invalid model type: " + type + ". Supported values: CHAT, ASR, TTS, EMBEDDING"
                    ));
                }
            } else if (providerId != null) {
                models = modelService.findActiveModelsByProviderId(providerId);
            } else {
                models = modelService.findAll();
            }

            List<ModelResponse> response = models.stream()
                    .filter(m -> m.getProvider() != null)
                    .map(ModelResponse::from)
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to list models: " + e.getMessage()
            ));
        }
    }
}
