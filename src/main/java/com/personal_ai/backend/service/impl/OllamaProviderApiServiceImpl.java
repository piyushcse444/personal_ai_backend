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
import java.time.Duration;

public class OllamaProviderApiServiceImpl implements ProviderApiService {

	private static final String DEFAULT_BASE_URL = "http://localhost:11434";
	private static final Duration TIMEOUT = Duration.ofMinutes(5);

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public OllamaProviderApiServiceImpl() {
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(TIMEOUT)
				.build();
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public ProviderApiResponse chat(java.util.List<Message> messages, Model model, ProviderConfig config) {
		String apiUrl = resolveBaseUrl(config);
		String modelName = model.getModelName();

		try {
			ObjectNode body = objectMapper.createObjectNode();
			body.put("model", modelName);
			body.put("stream", false);

			ArrayNode msgArray = body.putArray("messages");
			for (Message msg : messages) {
				ObjectNode node = msgArray.addObject();
				node.put("role", msg.getRole().name().toLowerCase());
				node.put("content", msg.getContent());
			}

			String jsonBody = objectMapper.writeValueAsString(body);

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(apiUrl + "/api/chat"))
					.timeout(TIMEOUT)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new RuntimeException("Ollama API error: " + response.statusCode() + " " + response.body());
			}

			return parseResponse(response.body());

		} catch (IOException e) {
			throw new RuntimeException("Failed to communicate with Ollama API", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Request interrupted", e);
		}
	}

	private ProviderApiResponse parseResponse(String responseBody) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			String content = root.path("message").path("content").asText();
			// Ollama doesn't always return token counts in non-streaming mode
			Integer inputTokens = root.path("prompt_eval_count").isInt() ? root.path("prompt_eval_count").asInt() : null;
			Integer outputTokens = root.path("eval_count").isInt() ? root.path("eval_count").asInt() : null;
			return ProviderApiResponse.builder()
					.content(content)
					.inputTokens(inputTokens)
					.outputTokens(outputTokens)
					.build();
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse Ollama response", e);
		}
	}

	private String resolveBaseUrl(ProviderConfig config) {
		if (config.getEndpointUrl() != null && !config.getEndpointUrl().isBlank()) {
			return config.getEndpointUrl();
		}
		return DEFAULT_BASE_URL;
	}
}
