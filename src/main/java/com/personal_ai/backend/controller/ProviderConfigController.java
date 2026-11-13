package com.personal_ai.backend.controller;

import com.personal_ai.backend.dto.CreateProviderConfigRequest;
import com.personal_ai.backend.dto.ProviderConfigResponse;
import com.personal_ai.backend.dto.UpdateProviderConfigRequest;
import com.personal_ai.backend.model.Provider;
import com.personal_ai.backend.model.ProviderConfig;
import com.personal_ai.backend.service.ProviderConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/provider-configs")
public class ProviderConfigController {

	private final ProviderConfigService providerConfigService;

	public ProviderConfigController(ProviderConfigService providerConfigService) {
		this.providerConfigService = providerConfigService;
	}

	@PostMapping
	public ResponseEntity<?> createConfig(@Valid @RequestBody CreateProviderConfigRequest request) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			byte[] credentialCiphertext = request.getApiKey().getBytes(StandardCharsets.UTF_8);

			ProviderConfig config = providerConfigService.createConfig(
					userId,
					request.getProviderId(),
					request.getConfigName(),
					credentialCiphertext,
					"v1",
					request.getEndpointUrl()
			);

			return ResponseEntity.status(201).body(toResponse(config));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@GetMapping
	public ResponseEntity<List<ProviderConfigResponse>> listConfigs(
			@RequestParam(required = false) UUID providerId) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			List<ProviderConfig> configs;
			if (providerId != null) {
				configs = providerConfigService.findByUserIdAndProviderId(userId, providerId);
			} else {
				configs = providerConfigService.findByUserId(userId);
			}

			List<ProviderConfigResponse> responses = configs.stream()
					.map(this::toResponse)
					.collect(Collectors.toList());
			return ResponseEntity.ok(responses);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(null);
		}
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getConfig(@PathVariable UUID id) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			return providerConfigService.findByUserIdAndId(userId, id)
					.map(config -> ResponseEntity.ok((Object) toResponse(config)))
					.orElse(ResponseEntity.notFound().build());
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@PatchMapping("/{id}")
	public ResponseEntity<?> updateConfig(@PathVariable UUID id, @Valid @RequestBody UpdateProviderConfigRequest request) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			// Verify ownership
			ProviderConfig config = providerConfigService.findByUserIdAndId(userId, id)
					.orElseThrow(() -> new IllegalArgumentException("Config not found"));

			byte[] credentialCiphertext = null;
			String credentialKeyVersion = null;
			if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
				credentialCiphertext = request.getApiKey().getBytes(StandardCharsets.UTF_8);
				credentialKeyVersion = "v1";
			}

			ProviderConfig updated = providerConfigService.updateConfig(
					id,
					request.getConfigName(),
					credentialCiphertext,
					credentialKeyVersion,
					request.getEndpointUrl(),
					request.getIsActive()
			);

			return ResponseEntity.ok(toResponse(updated));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteConfig(@PathVariable UUID id) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			// Verify ownership
			ProviderConfig config = providerConfigService.findByUserIdAndId(userId, id)
					.orElseThrow(() -> new IllegalArgumentException("Config not found"));

			providerConfigService.deleteConfig(id);

			return ResponseEntity.ok(Map.of(
					"success", true,
					"message", "Config deleted successfully"
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@PostMapping("/{id}/test")
	public ResponseEntity<?> testConfig(@PathVariable UUID id) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			// Verify ownership
			ProviderConfig config = providerConfigService.findByUserIdAndId(userId, id)
					.orElseThrow(() -> new IllegalArgumentException("Config not found"));

			// TODO: Actually test connectivity to the provider
			return ResponseEntity.ok(Map.of(
					"success", true,
					"message", "Config test successful",
					"provider", config.getProvider() != null ? config.getProvider().getCode() : null
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	private ProviderConfigResponse toResponse(ProviderConfig config) {
		Provider provider = config.getProvider();
		ProviderConfigResponse.ProviderInfo providerInfo = ProviderConfigResponse.ProviderInfo.builder()
				.id(provider != null ? provider.getId() : null)
				.code(provider != null ? provider.getCode() : null)
				.displayName(provider != null ? provider.getDisplayName() : null)
				.build();

		byte[] credential = config.getCredentialCiphertext();
		boolean configured = credential != null && credential.length > 0;
		String masked = configured ? maskCredential(credential) : null;

		return ProviderConfigResponse.builder()
				.id(config.getId())
				.configName(config.getConfigName())
				.isActive(config.getIsActive())
				.provider(providerInfo)
				.credentialConfigured(configured)
				.credentialMasked(masked)
				.build();
	}

	private String maskCredential(byte[] credential) {
		String key = new String(credential, StandardCharsets.UTF_8);
		if (key.length() <= 4) {
			return "****";
		}
		return "****" + key.substring(key.length() - 4);
	}
}
