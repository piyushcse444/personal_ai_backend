package com.personal_ai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequest {

	@Schema(description = "Message text content. Required if no image is provided.", example = "What is in this image?")
	private String content;

	@Schema(description = "Enable thinking/reasoning mode for the model. When true, reasoning-capable models will produce thinking content. When false, thinking is disabled. Defaults to true.", example = "true")
	private Boolean thinking = true;

	@Schema(description = "Base64-encoded image data (without the data: prefix, e.g. 'data:image/png;base64,' should be stripped). Only sent when the model supports vision.", example = "iVBORw0KGgoAAAANSUhEUg...")
	private String imageBase64;

	@Schema(description = "MIME type of the uploaded image, e.g. 'image/png' or 'image/jpeg'. Required if imageBase64 is provided.", example = "image/png")
	private String imageMimeType;
}
