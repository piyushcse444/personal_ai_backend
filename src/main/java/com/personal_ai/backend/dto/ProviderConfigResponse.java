package com.personal_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderConfigResponse {

	private UUID id;
	private String configName;
	private Boolean isActive;
	private ProviderInfo provider;
	private Boolean credentialConfigured;
	private String credentialMasked;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ProviderInfo {
		private UUID id;
		private String code;
		private String displayName;
	}
}
