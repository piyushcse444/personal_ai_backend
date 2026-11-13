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
public class MessageResponse {

	private UUID id;
	private String role;
	private String content;
	private Integer tokenCount;
	private OffsetDateTime createdAt;
}
