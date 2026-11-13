package com.personal_ai.backend.controller;

import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.model.Provider;
import com.personal_ai.backend.service.ModelService;
import com.personal_ai.backend.service.ProviderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ProviderController {

	private final ProviderService providerService;
	private final ModelService modelService;

	public ProviderController(ProviderService providerService, ModelService modelService) {
		this.providerService = providerService;
		this.modelService = modelService;
	}

	@GetMapping("/providers")
	public ResponseEntity<List<Provider>> listProviders() {
		return ResponseEntity.ok(providerService.findActiveProviders());
	}

	@GetMapping("/providers/{id}")
	public ResponseEntity<?> getProviderById(@PathVariable UUID id) {
		return providerService.findById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/providers/{id}/models")
	public ResponseEntity<List<Model>> getModelsByProviderId(@PathVariable UUID id) {
		List<Model> models = modelService.findActiveModelsByProviderId(id);
		return ResponseEntity.ok(models);
	}

	@GetMapping("/models")
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
	public ResponseEntity<?> getModelById(@PathVariable UUID id) {
		return modelService.findById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}
}
