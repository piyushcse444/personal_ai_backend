package com.personal_ai.backend.dto;

import com.personal_ai.backend.model.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request body for creating a new AI provider")
public class CreateProviderRequest {

	@NotBlank(message = "Provider code is required")
	@Schema(description = "Unique identifier code for the provider (e.g., 'openai', 'anthropic')", example = "openai", requiredMode = Schema.RequiredMode.REQUIRED)
	private String code;

	@NotBlank(message = "Display name is required")
	@Schema(description = "Human-readable display name for the provider", example = "OpenAI", requiredMode = Schema.RequiredMode.REQUIRED)
	private String displayName;

	@NotNull(message = "Provider type is required")
	@Schema(description = "Type of the provider: CLOUD, LOCAL, or SELF_HOSTED", example = "CLOUD", requiredMode = Schema.RequiredMode.REQUIRED)
	private Provider.ProviderType providerType;

	@Schema(description = "Optional base URL for the provider API endpoint", example = "https://api.openai.com/v1")
	private String baseUrl;
}
