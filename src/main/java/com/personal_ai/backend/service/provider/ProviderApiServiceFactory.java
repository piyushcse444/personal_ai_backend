package com.personal_ai.backend.service.provider;

import com.personal_ai.backend.service.model.impl.GoogleProviderApiServiceImpl;
import com.personal_ai.backend.service.model.impl.LlamaCppProviderApiServiceImpl;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ProviderApiServiceFactory {

	private final Map<String, ProviderApiService> providers;

	public ProviderApiServiceFactory(Optional<LlamaCppProviderApiServiceImpl> llamaCppProvider) {
		this.providers = new HashMap<>();
		providers.put("google", new GoogleProviderApiServiceImpl());
		llamaCppProvider.ifPresent(p -> providers.put("llamacpp", p));
	}

	/**
	 * Get the ProviderApiService implementation for the given provider code.
	 *
	 * @param providerCode the provider code (e.g., "openai", "anthropic", "google", "mistral", "ollama", "llamacpp")
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
