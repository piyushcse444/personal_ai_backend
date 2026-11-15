package com.personal_ai.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateConversationRequest {

	@NotNull(message = "Model ID is required")
	private UUID modelId;

	private UUID providerConfigId;

	private String title;
}
