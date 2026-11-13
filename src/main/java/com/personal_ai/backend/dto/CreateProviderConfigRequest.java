package com.personal_ai.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateProviderConfigRequest {

	@NotNull(message = "Provider ID is required")
	private UUID providerId;

	@NotBlank(message = "Config name is required")
	private String configName;

	@NotBlank(message = "API key is required")
	private String apiKey;

	private String endpointUrl;
}
