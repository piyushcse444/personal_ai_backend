package com.personal_ai.backend.service.provider;

import com.personal_ai.backend.service.model.impl.GoogleProviderApiServiceImpl;
import com.personal_ai.backend.service.model.impl.UnslothProviderApiServiceImpl;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ProviderApiServiceFactory {

	private final Map<String, ProviderApiService> providers;

	public ProviderApiServiceFactory(Optional<UnslothProviderApiServiceImpl> unslothProvider) {
		this.providers = new HashMap<>();
		providers.put("google", new GoogleProviderApiServiceImpl());
		unslothProvider.ifPresent(p -> providers.put("unsloth", p));
	}

	/**
	 * Get the ProviderApiService implementation for the given provider code.
	 *
	 * @param providerCode the provider code (e.g., "openai", "anthropic", "google", "mistral", "ollama", "unsloth")
	 * @return the corresponding ProviderApiService implementation
	 * @throws IllegalArgumentException if no implementation is found for the provider code
	 */
	public ProviderApiService getProvider(String providerCode) {
		ProviderApiService service = providers.get(providerCode.toLowerCase());
		if (service == null) {
			throw new IllegalArgumentException("No API implementation found for provider: " + providerCode);
		}
		return service;
	}
}
