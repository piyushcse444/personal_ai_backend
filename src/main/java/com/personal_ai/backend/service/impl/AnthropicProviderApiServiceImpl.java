package com.personal_ai.backend.service.impl;

import com.personal_ai.backend.dto.ProviderApiResponse;
import com.personal_ai.backend.model.Message;
import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.model.ProviderConfig;
import com.personal_ai.backend.service.ProviderApiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class AnthropicProviderApiServiceImpl implements ProviderApiService {

	private static final String BASE_URL = "https://api.anthropic.com";
	private static final String ANTHROPIC_VERSION = "2023-06-01";
	private static final Duration TIMEOUT = Duration.ofMinutes(2);

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public AnthropicProviderApiServiceImpl() {
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(TIMEOUT)
				.build();
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public ProviderApiResponse chat(java.util.List<Message> messages, Model model, ProviderConfig config) {
		String apiUrl = resolveBaseUrl(config);
		String apiKey = extractApiKey(config);
		String modelName = model.getModelName();

		try {
			ObjectNode body = objectMapper.createObjectNode();
			body.put("model", modelName);
			body.put("max_tokens", model.getContextWindow() != null ? model.getContextWindow() / 4 : 4096);

			ArrayNode msgArray = body.putArray("messages");
			for (Message msg : messages) {
				ObjectNode node = msgArray.addObject();
				node.put("role", msg.getRole().name().toLowerCase());
				node.put("content", msg.getContent());
			}

			String jsonBody = objectMapper.writeValueAsString(body);

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(apiUrl + "/v1/messages"))
					.timeout(TIMEOUT)
					.header("Content-Type", "application/json")
					.header("x-api-key", apiKey)
					.header("anthropic-version", ANTHROPIC_VERSION)
					.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new RuntimeException("Anthropic API error: " + response.statusCode() + " " + response.body());
			}

			return parseResponse(response.body());

		} catch (IOException e) {
			throw new RuntimeException("Failed to communicate with Anthropic API", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Request interrupted", e);
		}
	}

	private ProviderApiResponse parseResponse(String responseBody) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			// Anthropic returns content as an array of blocks
			String content = root.path("content").get(0).path("text").asText();
			Integer inputTokens = root.path("usage").path("input_tokens").isInt() ? root.path("usage").path("input_tokens").asInt() : null;
			Integer outputTokens = root.path("usage").path("output_tokens").isInt() ? root.path("usage").path("output_tokens").asInt() : null;
			return ProviderApiResponse.builder()
					.content(content)
					.inputTokens(inputTokens)
					.outputTokens(outputTokens)
					.build();
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse Anthropic response", e);
		}
	}

	private String resolveBaseUrl(ProviderConfig config) {
		if (config.getEndpointUrl() != null && !config.getEndpointUrl().isBlank()) {
			return config.getEndpointUrl();
		}
		return BASE_URL;
	}

	private String extractApiKey(ProviderConfig config) {
		if (config.getCredentialCiphertext() == null || config.getCredentialCiphertext().length == 0) {
			throw new IllegalArgumentException("API key not configured for this provider config");
		}
		return new String(config.getCredentialCiphertext(), StandardCharsets.UTF_8);
	}
}
