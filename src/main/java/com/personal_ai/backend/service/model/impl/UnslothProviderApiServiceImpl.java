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
import com.personal_ai.backend.service.model.UnslothServerManager;
import com.personal_ai.backend.service.provider.ProviderApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "app.unsloth.enabled", havingValue = "true")
public class UnslothProviderApiServiceImpl implements ProviderApiService {

	private static final Logger log = LoggerFactory.getLogger(UnslothProviderApiServiceImpl.class);

	private final UnslothServerManager serverManager;
	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final MessageService messageService;
	private final String baseUrl;
	private final String apiKey;
	private static final ExecutorService chatExecutor = Executors.newFixedThreadPool(
			4, r -> new Thread(r, "unsloth-chat"));

	public UnslothProviderApiServiceImpl(
			UnslothServerManager serverManager,
			MessageService messageService,
			@org.springframework.beans.factory.annotation.Value("${app.unsloth.base-url:http://127.0.0.1:8888}") String baseUrl,
			@org.springframework.beans.factory.annotation.Value("${app.unsloth.api-key:sk-unsloth-4203b2881dbe605f5363a3a333f63a22}") String apiKey) {
		this.serverManager = serverManager;
		this.messageService = messageService;
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
		// Use SimpleClientHttpRequestFactory (HTTP/1.1 only) to avoid "Unsupported upgrade request"
		// warnings from Unsloth server which doesn't support HTTP/2 upgrade (h2c ALPN)
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.simple().build();
		this.restClient = RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				.defaultHeader(org.springframework.http.HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.build();
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public ProviderApiResponse chat(List<Message> messages, Model model, ProviderConfig config, boolean thinking) {
		ensureModelReady(model.getModelName());

		String chatUrl = baseUrl + "/v1/chat/completions";
		List<ChatMessage> chatMessages = messages.stream()
				.map(m -> new ChatMessage(m.getRole().name().toLowerCase(), m.getContent(), null, null))
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
			UUID conversationId, UUID modelId, String imageBase64, String imageMimeType) {
		ensureModelReady(model.getModelName());

		SseEmitter emitter = new SseEmitter(0L);
		String chatUrl = baseUrl + "/v1/chat/completions";

		List<ChatMessage> chatMessages = messages.stream()
				.map(m -> new ChatMessage(m.getRole().name().toLowerCase(), m.getContent(), null, null))
				.collect(Collectors.toList());

		// If an image is attached, add it to the last user message
		if (imageBase64 != null && !imageBase64.isEmpty() && imageMimeType != null && !imageMimeType.isEmpty()) {
			// Find the last user message and append the image to its content
			for (int i = chatMessages.size() - 1; i >= 0; i--) {
				ChatMessage msg = chatMessages.get(i);
				if ("user".equals(msg.role())) {
					// Update the last user message to include the image
					chatMessages.set(i, new ChatMessage(msg.role(), msg.content(), imageBase64, imageMimeType));
					break;
				}
			}
			// If no user message found in history (shouldn't happen), add a new one
			if (imageBase64 != null) {
				chatMessages.add(new ChatMessage("user", imageBase64 != null && !imageBase64.isEmpty() ? imageBase64 : "", imageBase64, imageMimeType));
			}
		}

		ObjectNode requestBody = buildChatRequestBody(model.getModelName(), chatMessages, true, thinking, true);

		// Track whether the client is still connected — used to stop streaming early.
		AtomicBoolean clientConnected = new AtomicBoolean(true);

		emitter.onCompletion(() -> {
			clientConnected.set(false);
			log.debug("SSE client disconnected");
		});
		emitter.onTimeout(() -> {
			clientConnected.set(false);
			log.warn("SSE client timeout");
		});
		emitter.onError((e) -> {
			clientConnected.set(false);
			log.warn("SSE error", e);
		});

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
					if (clientConnected.get()) {
						emitter.send(SseEmitter.event()
								.name("error")
								.data(objectMapper.writeValueAsString(
										new ErrorResponse("Server returned HTTP " + responseCode))));
					}
					emitter.complete();
					return;
				}

				// Read SSE stream
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

					String line;
					StringBuilder responseContent = new StringBuilder();
					int totalOutputTokens = 0;

					while (clientConnected.get() && (line = reader.readLine()) != null) {
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
								if (clientConnected.get()) {
									emitter.send(SseEmitter.event()
											.name("done")
											.data(objectMapper.writeValueAsString(
													new DoneEvent(null, totalOutputTokens))));
								}
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

										if (!clientConnected.get()) {
											break;
										}

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

					// If we exit the loop without [DONE] and client is still connected, save and complete
					if (responseContent.length() > 0) {
						try {
							messageService.createMessage(conversationId, "ASSISTANT",
									responseContent.toString(), totalOutputTokens, modelId);
							log.debug("Saved assistant message to conversation {}", conversationId);
						} catch (Exception saveEx) {
							log.error("Failed to save assistant message", saveEx);
						}
					}
					if (clientConnected.get()) {
						emitter.send(SseEmitter.event()
								.name("done")
								.data(objectMapper.writeValueAsString(
										new DoneEvent(null, totalOutputTokens))));
						emitter.complete();
					}
				}

			} catch (IllegalStateException e) {
				// Emitter already completed — client likely disconnected
				log.debug("SSE emitter already completed (client disconnected): {}", e.getMessage());
			} catch (Exception e) {
				log.error("Error streaming from Unsloth API", e);
				try {
					if (clientConnected.get()) {
						emitter.send(SseEmitter.event()
								.name("error")
								.data(objectMapper.writeValueAsString(
										new ErrorResponse(e.getMessage()))));
					}
				} catch (Exception ex) {
					log.debug("Failed to send error event (client disconnected)", ex);
				}
				try {
					emitter.complete();
				} catch (Exception ex) {
					// Emitter may already be completed from onTimeout/onError
				}
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
		// Quant is resolved dynamically from the Unsloth API, not from config
		String modelIdentifier = modelName;
		if (!modelName.contains(":")) {
			String quant = serverManager.getQuantForModel(modelName);
			if (quant != null && !quant.isEmpty()) {
				modelIdentifier = modelName + ":" + quant;
			} else {
				log.warn("Could not resolve quant for model {}, using name without quant suffix", modelName);
			}
		}
		body.put("model", modelIdentifier);

		// Messages - build manually to support multi-content (text + image) format
		ArrayNode messagesArray = objectMapper.createArrayNode();
		for (ChatMessage msg : chatMessages) {
			ObjectNode msgNode = objectMapper.createObjectNode();
			msgNode.put("role", msg.role());

			// If the message has an image, use the multi-content array format
			if (msg.imageBase64() != null && !msg.imageBase64().isEmpty()) {
				ArrayNode contentArray = objectMapper.createArrayNode();
				// Add text part if present
				if (msg.content() != null && !msg.content().isEmpty()) {
					ObjectNode textPart = objectMapper.createObjectNode();
					textPart.put("type", "text");
					textPart.put("text", msg.content());
					contentArray.add(textPart);
				}
				// Add image part
				ObjectNode imagePart = objectMapper.createObjectNode();
				imagePart.put("type", "image_url");
				ObjectNode imageUrlNode = objectMapper.createObjectNode();
				String mimeType = msg.imageMimeType() != null ? msg.imageMimeType() : "image/png";
				imageUrlNode.put("url", "data:" + mimeType + ";base64," + msg.imageBase64());
				imagePart.set("image_url", imageUrlNode);
				contentArray.add(imagePart);
				msgNode.set("content", contentArray);
			} else {
				// Simple text-only message
				msgNode.put("content", msg.content() != null ? msg.content() : "");
			}
			messagesArray.add(msgNode);
		}
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

	private void ensureModelReady(String modelName) {
		boolean ready = serverManager.ensureModelReady(modelName);
		if (!ready) {
			log.error("Failed to ensure model {} is ready for inference", modelName);
			throw new IllegalStateException("Model " + modelName + " is not ready for inference. Please check if Unsloth Studio is running.");
		}
	}

	// Request/Response records for the OpenAI-compatible API
	// imageBase64 and imageMimeType are used for vision models (e.g., Gemma-4-E4B)
	private record ChatMessage(String role, String content, String imageBase64, String imageMimeType) {}

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
