package com.personal_ai.backend.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateConversationRequest {

	private String title;
	private UUID modelId;
	private UUID providerConfigId;
}
