package com.personal_ai.backend.service.impl;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import java.util.Collections;
import com.personal_ai.backend.dto.ProviderApiResponse;
import com.personal_ai.backend.model.Message;
import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.model.ProviderConfig;
import com.personal_ai.backend.service.ProviderApiService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GoogleProviderApiServiceImpl implements ProviderApiService {

	@Override
	public ProviderApiResponse chat(List<Message> messages, Model model, ProviderConfig config) {
		String apiKey = extractApiKey(config);
		String modelName = model.getModelName();

		// Create client with the API key from the provider config
		Client geminiClient = new Client.Builder()
				.apiKey(apiKey)
				.build();

		// Build the content list from message history
		List<Content> contents = new ArrayList<>();
		for (Message msg : messages) {
			String role = msg.getRole().name().toLowerCase();
			// Gemini uses "user" and "model" instead of "assistant"
			String geminiRole = "assistant".equals(role) ? "model" : role;
			Content content = Content.builder()
					.role(geminiRole)
					.parts(Collections.singletonList(Part.fromText(msg.getContent())))
					.build();
			contents.add(content);
		}

		// Configure generation parameters
		GenerateContentConfig generationConfig = GenerateContentConfig.builder()
				.maxOutputTokens(model.getContextWindow() != null ? model.getContextWindow() / 4 : 4096)
				.build();

		try {
			// Send the request to Gemini
			GenerateContentResponse response = geminiClient.models.generateContent(
					modelName,
					contents,
					generationConfig
				);

			// Extract the response text
			String content = response.text();

			// Extract token usage if available
			Integer inputTokens = null;
			Integer outputTokens = null;
			var usageOpt = response.usageMetadata();
			if (usageOpt.isPresent()) {
				var usage = usageOpt.get();
				inputTokens = usage.promptTokenCount().orElse(null);
				outputTokens = usage.candidatesTokenCount().orElse(null);
			}

			return ProviderApiResponse.builder()
					.content(content)
					.inputTokens(inputTokens)
					.outputTokens(outputTokens)
					.build();

		} catch (Exception e) {
			throw new RuntimeException("Failed to communicate with Google AI API: " + e.getMessage(), e);
		}
	}

	private String extractApiKey(ProviderConfig config) {
		if (config.getCredentialCiphertext() == null || config.getCredentialCiphertext().length == 0) {
			throw new IllegalArgumentException("API key not configured for this provider config");
		}
		return new String(config.getCredentialCiphertext(), StandardCharsets.UTF_8);
	}
}
