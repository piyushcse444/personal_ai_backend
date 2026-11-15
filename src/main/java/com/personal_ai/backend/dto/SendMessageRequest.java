package com.personal_ai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequest {

	@NotBlank(message = "Message content is required")
	private String content;

	@Schema(description = "Enable thinking/reasoning mode for the model. When true, reasoning-capable models will produce thinking content. When false, thinking is disabled. Defaults to true.", example = "true")
	private Boolean thinking = true;
}
