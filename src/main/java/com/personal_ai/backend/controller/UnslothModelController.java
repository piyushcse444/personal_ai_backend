package com.personal_ai.backend.controller;

import com.personal_ai.backend.service.model.UnslothServerManager;
import com.personal_ai.backend.service.model.UnslothServerManager.InferenceStatus;
import com.personal_ai.backend.service.model.UnslothServerManager.ModelInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/unsloth")
@ConditionalOnBean(UnslothServerManager.class)
@Tag(name = "Unsloth Model Management", description = "APIs to manage Unsloth model loading/unloading at runtime")
public class UnslothModelController {

	private final UnslothServerManager serverManager;

	public UnslothModelController(UnslothServerManager serverManager) {
		this.serverManager = serverManager;
	}

	@PostMapping("/models/unload")
	@Operation(summary = "Unload a model")
	@ApiResponse(responseCode = "200", description = "Model unloaded")
	public ResponseEntity<?> unloadModel(@RequestBody UnloadModelRequest request) {
		String modelId = request.getModelId();
		serverManager.unloadModel(modelId);
		return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "Model " + modelId + " unloaded successfully",
				"model", modelId
		));
	}

	public static class UnloadModelRequest {
		private String modelId;

		public String getModelId() {
			return modelId;
		}

		public void setModelId(String modelId) {
			this.modelId = modelId;
		}
	}

	@GetMapping("/models")
	@Operation(summary = "List Unsloth models", description = "List all models available in Unsloth Studio with their load status.")
	public ResponseEntity<List<ModelInfo>> listModels() {
		List<ModelInfo> models = serverManager.listModels();
		return ResponseEntity.ok(models);
	}

	@GetMapping("/status")
	@Operation(summary = "Get inference status", description = "Get the current inference status including loaded and serving models.")
	public ResponseEntity<?> getInferenceStatus() {
		InferenceStatus status = serverManager.getInferenceStatus();
		if (status != null) {
			return ResponseEntity.ok(status);
		} else {
			return ResponseEntity.internalServerError().body(Map.of(
					"success", false,
					"message", "Failed to get inference status or Unsloth API is unavailable"
			));
		}
	}

	@GetMapping("/health")
	@Operation(summary = "Check Unsloth health", description = "Check if the Unsloth API service is healthy and responsive.")
	public ResponseEntity<?> checkHealth() {
		boolean healthy = serverManager.checkHealth();
		if (healthy) {
			return ResponseEntity.ok(Map.of(
					"success", true,
					"status", "healthy",
					"message", "Unsloth API service is healthy"
			));
		} else {
			return ResponseEntity.internalServerError().body(Map.of(
					"success", false,
					"status", "unhealthy",
					"message", "Unsloth API service is not healthy or unreachable"
			));
		}
	}
}
