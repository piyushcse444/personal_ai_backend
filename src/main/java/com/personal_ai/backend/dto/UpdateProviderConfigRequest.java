package com.personal_ai.backend.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateProviderConfigRequest {

	private String configName;
	private String apiKey;
	private String endpointUrl;
	private Boolean isActive;
}
