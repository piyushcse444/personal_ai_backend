package com.personal_ai.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRequest {

	@NotBlank(message = "Display name is required")
	private String displayName;
}
