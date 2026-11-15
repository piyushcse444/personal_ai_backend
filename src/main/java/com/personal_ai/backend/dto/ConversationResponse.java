package com.personal_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

	private UUID id;
	private String title;
	private ModelInfo model;
	private OffsetDateTime updatedAt;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ModelInfo {
		private UUID id;
		private String modelName;
		private String displayName;
		private Boolean supportsVision;
	}
}
