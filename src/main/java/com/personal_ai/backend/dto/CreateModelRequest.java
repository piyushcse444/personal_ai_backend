package com.personal_ai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Request body for creating a new AI model")
public class CreateModelRequest {

	@NotNull(message = "Provider ID is required")
	@Schema(description = "UUID of the provider this model belongs to", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
	private UUID providerId;

	@NotBlank(message = "Model name is required")
	@Schema(description = "Unique model identifier (e.g., 'gpt-4', 'claude-3-opus')", example = "gpt-4", requiredMode = Schema.RequiredMode.REQUIRED)
	private String modelName;

	@NotBlank(message = "Display name is required")
	@Schema(description = "Human-readable display name for the model", example = "GPT-4", requiredMode = Schema.RequiredMode.REQUIRED)
	private String displayName;

	@NotNull(message = "Context window is required")
	@Min(value = 1, message = "Context window must be positive")
	@Schema(description = "Context window size in tokens", example = "8192", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer contextWindow;
}
