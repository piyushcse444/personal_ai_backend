package com.personal_ai.backend.service;

import com.personal_ai.backend.service.impl.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ProviderApiServiceFactory {

	private final Map<String, ProviderApiService> providers;

	public ProviderApiServiceFactory() {
		this.providers = new HashMap<>();
		providers.put("openai", new OpenAiProviderApiServiceImpl());
		providers.put("anthropic", new AnthropicProviderApiServiceImpl());
		providers.put("google", new GoogleProviderApiServiceImpl());
		providers.put("mistral", new MistralProviderApiServiceImpl());
		providers.put("ollama", new OllamaProviderApiServiceImpl());
	}

	/**
	 * Get the ProviderApiService implementation for the given provider code.
	 *
	 * @param providerCode the provider code (e.g., "openai", "anthropic", "google", "mistral", "ollama")
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
