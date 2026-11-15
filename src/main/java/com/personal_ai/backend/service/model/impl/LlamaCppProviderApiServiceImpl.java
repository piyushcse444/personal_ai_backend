package com.personal_ai.backend.service.model.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.personal_ai.backend.dto.ProviderApiResponse;
import com.personal_ai.backend.model.Message;
import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.model.ProviderConfig;
import com.personal_ai.backend.service.conversation.MessageService;
import com.personal_ai.backend.service.model.LlamaCppServerManager;
import com.personal_ai.backend.service.provider.ProviderApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "app.llamacpp.enabled", havingValue = "true")
public class LlamaCppProviderApiServiceImpl implements ProviderApiService {

	private static final Logger log = LoggerFactory.getLogger(LlamaCppProviderApiServiceImpl.class);

	private final LlamaCppServerManager serverManager;
	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final MessageService messageService;
	private final String baseUrl;
	private final String apiKey;
	private final String modelPath;
	private static final ExecutorService chatExecutor = Executors.newFixedThreadPool(
			4, r -> new Thread(r, "unsloth-chat"));

	public LlamaCppProviderApiServiceImpl(
			LlamaCppServerManager serverManager,
			MessageService messageService,
			@org.springframework.beans.factory.annotation.Value("${app.llamacpp.base-url:http://127.0.0.1:8888}") String baseUrl,
			@org.springframework.beans.factory.annotation.Value("${app.llamacpp.api-key:sk-unsloth-4203b2881dbe605f5363a3a333f63a22}") String apiKey,
			@org.springframework.beans.factory.annotation.Value("${app.llamacpp.model:AtomicChat/LFM3-1.7B-Uncensored-GGUF}") String modelPath) {
		this.serverManager = serverManager;
		this.messageService = messageService;
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
		this.modelPath = modelPath;
		this.restClient = RestClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader(org.springframework.http.HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.build();
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public ProviderApiResponse chat(List<Message> messages, Model model, ProviderConfig config, boolean thinking) {
		ensureModelReady();

		String chatUrl = baseUrl + "/v1/chat/completions";
		List<ChatMessage> chatMessages = messages.stream()
				.map(m -> new ChatMessage(m.getRole().name().toLowerCase(), m.getContent()))
				.collect(Collectors.toList());

		ObjectNode requestBody = buildChatRequestBody(model.getModelName(), chatMessages, false, thinking, false);

		try {
			ChatResponse response = restClient.post()
					.uri(chatUrl)
					.contentType(MediaType.APPLICATION_JSON)
					.body(requestBody)
					.retrieve()
					.body(ChatResponse.class);

			if (response == null || response.choices() == null || response.choices().isEmpty()) {
				throw new IllegalStateException("Unsloth API returned an empty response");
			}

			String content = response.choices().get(0).message().content();
			Integer inputTokens = response.usage() != null ? response.usage().promptTokens() : null;
			Integer outputTokens = response.usage() != null ? response.usage().completionTokens() : null;

			return ProviderApiResponse.builder()
					.content(content)
					.inputTokens(inputTokens)
					.outputTokens(outputTokens)
					.build();

		} catch (Exception e) {
			log.error("Error calling Unsloth chat API", e);
			throw new RuntimeException("Failed to get response from Unsloth API: " + e.getMessage(), e);
		}
	}

	/**
	 * Stream chat completions from the Unsloth API via SSE.
	 * Parses thinking/reasoning content from the model output.
	 * After streaming completes, saves only the final response (non-thinking) to the database.
	 * Thinking content is streamed to the frontend but not persisted.
	 */
	public SseEmitter chatStreaming(List<Message> messages, Model model, ProviderConfig config, boolean thinking,
			UUID conversationId, UUID modelId) {
		ensureModelReady();

		SseEmitter emitter = new SseEmitter(0L);
		String chatUrl = baseUrl + "/v1/chat/completions";

		List<ChatMessage> chatMessages = messages.stream()
				.map(m -> new ChatMessage(m.getRole().name().toLowerCase(), m.getContent()))
				.collect(Collectors.toList());

		ObjectNode requestBody = buildChatRequestBody(model.getModelName(), chatMessages, true, thinking, true);

		// Complete emitter on client disconnect
		emitter.onCompletion(() -> log.debug("SSE client disconnected"));
		emitter.onTimeout(() -> log.warn("SSE client timeout"));
		emitter.onError((e) -> log.warn("SSE error", e));

		Thread streamingThread = new Thread(() -> {
			HttpURLConnection connection = null;
			try {
				URI uri = URI.create(chatUrl);
				connection = (HttpURLConnection) uri.toURL().openConnection();
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setDoOutput(true);
				connection.setConnectTimeout(10000);
				connection.setReadTimeout(0);

				// Add Bearer token authentication
				connection.setRequestProperty("Authorization", "Bearer " + apiKey);

				// Write request body
				try (var os = connection.getOutputStream()) {
					os.write(objectMapper.writeValueAsBytes(requestBody));
					os.flush();
				}

				int responseCode = connection.getResponseCode();
				if (responseCode != 200) {
					emitter.send(SseEmitter.event()
							.name("error")
							.data(objectMapper.writeValueAsString(
									new ErrorResponse("Server returned HTTP " + responseCode))));
					emitter.complete();
					return;
				}

				// Read SSE stream
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

					String line;
					StringBuilder responseContent = new StringBuilder();
					int totalOutputTokens = 0;

					while ((line = reader.readLine()) != null) {
						if (line.startsWith("data: ")) {
							String data = line.substring(6);
							if ("[DONE]".equals(data)) {
								// Save only the final response (non-thinking) to the database
								if (responseContent.length() > 0) {
									try {
										messageService.createMessage(conversationId, "ASSISTANT",
												responseContent.toString(), totalOutputTokens, modelId);
										log.debug("Saved assistant message to conversation {}", conversationId);
									} catch (Exception saveEx) {
										log.error("Failed to save assistant message", saveEx);
									}
								}
								// Send final token counts
								emitter.send(SseEmitter.event()
										.name("done")
										.data(objectMapper.writeValueAsString(
												new DoneEvent(null, totalOutputTokens))));
								emitter.complete();
								return;
							}

							try {
								JsonNode json = objectMapper.readTree(data);
								JsonNode choices = json.get("choices");
								if (choices != null && choices.size() > 0) {
									JsonNode delta = choices.get(0).get("delta");
									if (delta != null) {
										String content = (delta.has("content") && !delta.get("content").isNull())
												? delta.get("content").asText() : "";
										String reasoningContent = (delta.has("reasoning_content") && !delta.get("reasoning_content").isNull())
												? delta.get("reasoning_content").asText() : "";

										if (reasoningContent != null && !reasoningContent.isEmpty()) {
											// Only stream thinking content to frontend if thinking was requested
											if (thinking) {
												emitter.send(SseEmitter.event()
														.name("token")
														.data(objectMapper.writeValueAsString(
																new TokenEvent(reasoningContent, true))));
											}
											totalOutputTokens++;
										} else if (content != null && !content.isEmpty()) {
											// Regular content — stream to frontend and save to DB
											emitter.send(SseEmitter.event()
													.name("token")
													.data(objectMapper.writeValueAsString(
															new TokenEvent(content, false))));
											responseContent.append(content);
											totalOutputTokens++;
										}
									}
								}

								// Track usage
								JsonNode usage = json.get("usage");
								if (usage != null && usage.has("prompt_tokens")) {
									// Final chunk with usage
								}
							} catch (Exception e) {
								log.debug("Failed to parse SSE chunk: {}", line, e);
							}
						}
					}

					// If we exit the loop without [DONE], still save and complete
					if (responseContent.length() > 0) {
						try {
							messageService.createMessage(conversationId, "ASSISTANT",
									responseContent.toString(), totalOutputTokens, modelId);
							log.debug("Saved assistant message to conversation {}", conversationId);
						} catch (Exception saveEx) {
							log.error("Failed to save assistant message", saveEx);
						}
					}
					emitter.send(SseEmitter.event()
							.name("done")
							.data(objectMapper.writeValueAsString(
									new DoneEvent(null, totalOutputTokens))));
					emitter.complete();
				}

			} catch (Exception e) {
				log.error("Error streaming from Unsloth API", e);
				try {
					emitter.send(SseEmitter.event()
							.name("error")
							.data(objectMapper.writeValueAsString(
									new ErrorResponse(e.getMessage()))));
				} catch (Exception ex) {
					log.warn("Failed to send error event", ex);
				}
				emitter.completeWithError(e);
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}
		}, "unsloth-streaming");
		streamingThread.setDaemon(true);
		streamingThread.start();

		return emitter;
	}

	private ObjectNode buildChatRequestBody(String modelName, List<ChatMessage> chatMessages, boolean stream, boolean thinking, boolean enableTools) {
		ObjectNode body = objectMapper.createObjectNode();

		// Model - use format "model_path:quant" for chat API
		String modelIdentifier = modelName;
		if (!modelName.contains(":")) {
			// Try to get the quant from the server manager
			List<LlamaCppServerManager.ModelInfo> models = serverManager.listModels();
			for (LlamaCppServerManager.ModelInfo m : models) {
				if (m.id().equals(modelName) || m.id().startsWith(modelName)) {
					if (m.quant() != null && !m.quant().isEmpty()) {
						modelIdentifier = modelName + ":" + m.quant();
					}
					break;
				}
			}
			if (modelIdentifier.equals(modelName)) {
				modelIdentifier = modelName + ":Q4_K_M";
			}
		}
		body.put("model", modelIdentifier);

		// Messages
		JsonNode messagesArray = objectMapper.valueToTree(chatMessages);
		body.set("messages", messagesArray);

		// Generation parameters
		body.put("temperature", 0.7);
		body.put("top_p", 0.8);
		body.put("top_k", 20);
		body.put("min_p", 0.05);
		body.put("repetition_penalty", 1.1);
		body.put("max_tokens", 1024);

		// Thinking and tools
		body.put("enable_thinking", thinking);
		body.put("enable_tools", enableTools);

		// Enabled tools array
		ArrayNode toolsArray = objectMapper.createArrayNode();
		if (enableTools) {
			toolsArray.add("web_search");
			toolsArray.add("python");
			toolsArray.add("terminal");
		}
		body.set("enabled_tools", toolsArray);

		body.put("permission_mode", "off");

		// Streaming
		body.put("stream", stream);

		return body;
	}

	private void ensureModelReady() {
		boolean ready = serverManager.ensureModelReady(modelPath);
		if (!ready) {
			log.error("Failed to ensure model {} is ready for inference", modelPath);
			throw new IllegalStateException("Model is not ready for inference. Please check if Unsloth Studio is running.");
		}
	}

	// Request/Response records for the OpenAI-compatible API
	private record ChatMessage(String role, String content) {}

	private record ChatResponse(
			List<ChatChoice> choices,
			Usage usage
		) {}

	private record ChatChoice(ChatMessageRecord message) {}

	private record ChatMessageRecord(String role, String content) {}

	private record Usage(Integer promptTokens, Integer completionTokens) {}

	// SSE event records
	private record TokenEvent(String content, boolean isThinking) {}
	private record DoneEvent(Integer inputTokens, Integer outputTokens) {}
	private record ErrorResponse(String message) {}
}
