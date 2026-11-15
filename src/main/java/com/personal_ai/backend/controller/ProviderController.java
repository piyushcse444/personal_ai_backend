package com.personal_ai.backend.controller;

import com.personal_ai.backend.dto.CreateModelRequest;
import com.personal_ai.backend.dto.CreateProviderRequest;
import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.model.Provider;
import com.personal_ai.backend.service.model.ModelService;
import com.personal_ai.backend.service.provider.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Provider & Model", description = "AI provider and model catalog APIs (public access)")
public class ProviderController {

	private final ProviderService providerService;
	private final ModelService modelService;

	public ProviderController(ProviderService providerService, ModelService modelService) {
		this.providerService = providerService;
		this.modelService = modelService;
	}

	@GetMapping("/providers")
	@Operation(summary = "List providers", description = "List all active AI providers available in the system")
	public ResponseEntity<List<Provider>> listProviders() {
		return ResponseEntity.ok(providerService.findActiveProviders());
	}

	@GetMapping("/providers/{id}")
	@Operation(summary = "Get provider by ID", description = "Retrieve details of a specific AI provider by its ID")
	public ResponseEntity<?> getProviderById(@PathVariable UUID id) {
		return providerService.findById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/providers")
	@Operation(summary = "Create provider", description = "Create a new AI provider in the system")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Provider created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request or provider code already exists")
	})
	public ResponseEntity<?> createProvider(
			@Parameter(description = "Provider creation request with code, display name, type, and optional base URL", required = true)
			@Valid @RequestBody CreateProviderRequest request) {
		try {
			Provider provider = providerService.createProvider(
					request.getCode(),
					request.getDisplayName(),
					request.getProviderType(),
					request.getBaseUrl()
			);

			return ResponseEntity.status(201).body(provider);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@DeleteMapping("/providers/{id}")
	@Operation(summary = "Delete provider", description = "Permanently delete an AI provider from the system")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Provider deleted successfully"),
			@ApiResponse(responseCode = "400", description = "Provider not found"),
			@ApiResponse(responseCode = "404", description = "Provider does not exist")
	})
	public ResponseEntity<?> deleteProvider(
			@Parameter(description = "UUID of the provider to delete", required = true)
			@PathVariable UUID id) {
		try {
			providerService.deleteProvider(id);

			return ResponseEntity.ok(Map.of(
					"success", true,
					"message", "Provider deleted successfully"
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@GetMapping("/providers/{id}/models")
	@Operation(summary = "Get models by provider", description = "List all active models available for a specific AI provider")
	public ResponseEntity<List<Model>> getModelsByProviderId(@PathVariable UUID id) {
		List<Model> models = modelService.findActiveModelsByProviderId(id);
		return ResponseEntity.ok(models);
	}

	@GetMapping("/models")
	@Operation(summary = "List models", description = "List all available AI models, optionally filtered by provider ID")
	public ResponseEntity<List<Model>> listModels(@RequestParam(required = false) UUID providerId) {
		List<Model> models;
		if (providerId != null) {
			models = modelService.findActiveModelsByProviderId(providerId);
		} else {
			models = modelService.findAll();
		}
		return ResponseEntity.ok(models);
	}

	@GetMapping("/models/{id}")
	@Operation(summary = "Get model by ID", description = "Retrieve details of a specific AI model by its ID")
	public ResponseEntity<?> getModelById(@PathVariable UUID id) {
		return modelService.findById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/models")
	@Operation(summary = "Create model", description = "Create a new AI model under an existing provider")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Model created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request or provider not found")
	})
	public ResponseEntity<?> createModel(
			@Parameter(description = "Model creation request with provider ID, model name, display name, and context window", required = true)
			@Valid @RequestBody CreateModelRequest request) {
		try {
			Model model = modelService.createModel(
					request.getProviderId(),
					request.getModelName(),
					request.getDisplayName(),
					request.getContextWindow()
			);

			return ResponseEntity.status(201).body(model);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@DeleteMapping("/models/{id}")
	@Operation(summary = "Delete model", description = "Permanently delete an AI model from the system")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Model deleted successfully"),
			@ApiResponse(responseCode = "400", description = "Model not found"),
			@ApiResponse(responseCode = "404", description = "Model does not exist")
	})
	public ResponseEntity<?> deleteModel(
			@Parameter(description = "UUID of the model to delete", required = true)
			@PathVariable UUID id) {
		try {
			modelService.deleteModel(id);

			return ResponseEntity.ok(Map.of(
					"success", true,
					"message", "Model deleted successfully"
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@DeleteMapping("/models")
	@Operation(summary = "Delete all models", description = "Delete all AI models from the system")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "All models deleted successfully")
	})
	public ResponseEntity<?> deleteAllModels() {
		long count = modelService.deleteAllModels();

		return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "All models deleted successfully",
				"deletedCount", count
		));
	}
}
