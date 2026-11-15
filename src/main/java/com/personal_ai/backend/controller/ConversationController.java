package com.personal_ai.backend.controller;

import com.personal_ai.backend.dto.*;
import com.personal_ai.backend.model.Conversation;
import com.personal_ai.backend.model.Message;
import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.model.Provider;
import com.personal_ai.backend.model.ProviderConfig;
import com.personal_ai.backend.service.conversation.ConversationService;
import com.personal_ai.backend.service.conversation.MessageService;
import com.personal_ai.backend.service.provider.ProviderApiService;
import com.personal_ai.backend.service.provider.ProviderApiServiceFactory;
import com.personal_ai.backend.service.provider.ProviderConfigService;
import com.personal_ai.backend.service.model.ModelService;
import com.personal_ai.backend.service.model.impl.UnslothProviderApiServiceImpl;
import com.personal_ai.backend.repository.ConversationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversation", description = "Conversation and message management APIs")
public class ConversationController {

	private final ConversationService conversationService;
	private final MessageService messageService;
	private final ProviderConfigService providerConfigService;
	private final ProviderApiServiceFactory providerApiServiceFactory;
	private final ConversationRepository conversationRepository;
	private final ModelService modelService;
	private final java.util.Optional<UnslothProviderApiServiceImpl> unslothProvider;

	public ConversationController(ConversationService conversationService, MessageService messageService,
								ProviderConfigService providerConfigService,
								ProviderApiServiceFactory providerApiServiceFactory,
								ConversationRepository conversationRepository,
								ModelService modelService,
								java.util.Optional<UnslothProviderApiServiceImpl> unslothProvider) {
		this.conversationService = conversationService;
		this.messageService = messageService;
		this.providerConfigService = providerConfigService;
		this.providerApiServiceFactory = providerApiServiceFactory;
		this.conversationRepository = conversationRepository;
		this.modelService = modelService;
		this.unslothProvider = unslothProvider;
	}

	@PostMapping
	@Operation(summary = "Create conversation", description = "Create a new conversation with a specified AI model and optional provider configuration (not required for offline models)")
	public ResponseEntity<?> createConversation(@Valid @RequestBody CreateConversationRequest request) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			UUID providerConfigId = request.getProviderConfigId();

			if (providerConfigId != null) {
				// Online model: provider config is required
				var configOpt = providerConfigService.findById(providerConfigId);
				UUID providerId = configOpt.map(c -> c.getProvider().getId()).orElse(null);

				Conversation conversation = conversationService.createConversation(
						userId,
						providerId,
						request.getModelId(),
						providerConfigId,
						request.getTitle()
				);

				return ResponseEntity.status(201).body(toResponse(conversation));
			} else {
				// Offline model: no provider config needed; get provider from the model
				var modelOpt = modelService.findById(request.getModelId());
				UUID providerId = modelOpt.map(m -> m.getProvider().getId()).orElse(null);

				Conversation conversation = conversationService.createConversation(
						userId,
						providerId,
						request.getModelId(),
						request.getTitle()
				);

				return ResponseEntity.status(201).body(toResponse(conversation));
			}
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@GetMapping
	@Operation(summary = "List conversations", description = "List all conversations for the authenticated user, optionally filtered by search query")
	public ResponseEntity<List<ConversationResponse>> listConversations(
			@RequestParam(required = false) String search) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			List<Conversation> conversations;
			if (search != null && !search.isBlank()) {
				conversations = conversationRepository.findByUserIdAndTitleContaining(userId, search);
			} else {
				conversations = conversationService.findByUserId(userId);
			}

			List<ConversationResponse> responses = conversations.stream()
					.map(this::toResponse)
					.collect(Collectors.toList());
			return ResponseEntity.ok(responses);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(null);
		}
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get conversation", description = "Retrieve a specific conversation by ID")
	public ResponseEntity<?> getConversation(@PathVariable UUID id) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			return conversationService.findByUserIdAndId(userId, id)
					.map(conv -> ResponseEntity.ok((Object) toResponse(conv)))
					.orElse(ResponseEntity.notFound().build());
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Update conversation", description = "Update an existing conversation's title, model, or provider configuration")
	public ResponseEntity<?> updateConversation(@PathVariable UUID id, @Valid @RequestBody UpdateConversationRequest request) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			// Verify ownership
			Conversation conversation = conversationService.findByUserIdAndId(userId, id)
					.orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

			UUID providerId = conversation.getProvider() != null ? conversation.getProvider().getId() : null;
			Conversation updated = conversationService.updateConversation(
					id,
					request.getTitle(),
					providerId,
					request.getModelId(),
					request.getProviderConfigId()
			);

			return ResponseEntity.ok(toResponse(updated));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete conversation", description = "Permanently delete a conversation and all its messages")
	public ResponseEntity<?> deleteConversation(@PathVariable UUID id) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			// Verify ownership
			Conversation conversation = conversationService.findByUserIdAndId(userId, id)
					.orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

			conversationService.deleteConversation(id);

			return ResponseEntity.ok(Map.of(
					"success", true,
					"message", "Conversation deleted successfully"
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@GetMapping("/{id}/messages")
	@Operation(summary = "Get messages", description = "Retrieve all messages in a conversation, ordered by creation time")
	public ResponseEntity<?> getMessages(@PathVariable UUID id) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			// Verify ownership
			Conversation conversation = conversationService.findByUserIdAndId(userId, id)
					.orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

			List<Message> messages = messageService.findByConversationIdOrderByCreatedAtAsc(id);
			List<MessageResponse> responses = messages.stream()
					.map(this::toMessageResponse)
					.collect(Collectors.toList());
			return ResponseEntity.ok(responses);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@PostMapping("/{id}/messages")
	@Operation(summary = "Send message", description = "Send a message to a conversation and receive an AI assistant response")
	public ResponseEntity<?> sendMessage(@PathVariable UUID id, @Valid @RequestBody SendMessageRequest request) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);
			System.out.println(LocalDateTime.now());
			// Verify ownership
			Conversation conversation = conversationService.findByUserIdAndId(userId, id)
					.orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

			Model model = conversation.getModel();
			ProviderConfig providerConfig = conversation.getProviderConfig();
			Provider provider = conversation.getProvider();

			// Save user message
			Message userMessage = messageService.createMessage(id, "USER", request.getContent(), null, model.getId());
			System.out.println(LocalDateTime.now());
			// Get conversation history for context
			List<Message> history = messageService.findByConversationIdOrderByCreatedAtAsc(id);

			// Call the provider API
			ProviderApiService apiService = providerApiServiceFactory.getProvider(provider.getCode());
			boolean thinking = request.getThinking() != null && request.getThinking();
			ProviderApiResponse apiResponse = apiService.chat(history, model, providerConfig, thinking);

			// Calculate total token count
			Integer tokenCount = null;
			if (apiResponse.getOutputTokens() != null) {
				tokenCount = apiResponse.getOutputTokens();
			}

			// Save assistant response
			Message assistantMessage = messageService.createMessage(
					id, "ASSISTANT", apiResponse.getContent(), tokenCount, model.getId()
			);

			return ResponseEntity.ok(Map.of(
					"success", true,
					"userMessage", toMessageResponse(userMessage),
					"assistantMessage", toMessageResponse(assistantMessage)
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", "Failed to get response from AI provider: " + e.getMessage()
			));
		}
	}

	@PostMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Send message with streaming", description = "Send a message and stream the AI assistant response token by token via SSE")
	public SseEmitter sendMessageStream(@PathVariable UUID id, @Valid @RequestBody SendMessageRequest request) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			// Verify ownership
			Conversation conversation = conversationService.findByUserIdAndId(userId, id)
					.orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

			Model model = conversation.getModel();
			ProviderConfig providerConfig = conversation.getProviderConfig();
			Provider provider = conversation.getProvider();

			// Save user message (include image indicator if an image was uploaded)
			String userContent = request.getContent();
			if (request.getImageBase64() != null && !request.getImageBase64().isEmpty()) {
				// Prepend an image indicator so the chat history shows an image was sent
				String prefix = "[Image uploaded]";
				userContent = (userContent != null && !userContent.isEmpty())
						? prefix + " " + userContent
						: prefix;
			}
			messageService.createMessage(id, "USER", userContent, null, model.getId());

			// Get conversation history for context
			List<Message> history = messageService.findByConversationIdOrderByCreatedAtAsc(id);

  		// Use streaming for unsloth provider, fallback to sync for others
 			boolean thinking = request.getThinking() != null && request.getThinking();
 			if ("unsloth".equalsIgnoreCase(provider.getCode()) && unslothProvider.isPresent()) {
 				String imageBase64 = request.getImageBase64();
 				String imageMimeType = request.getImageMimeType();
 				return unslothProvider.get().chatStreaming(history, model, providerConfig, thinking, id, model.getId(), imageBase64, imageMimeType);
 			}

 			// Fallback: synchronous response wrapped in a single SSE event
 			ProviderApiService apiService = providerApiServiceFactory.getProvider(provider.getCode());
 			ProviderApiResponse apiResponse = apiService.chat(history, model, providerConfig, thinking);

			Integer tokenCount = apiResponse.getOutputTokens();
			messageService.createMessage(id, "ASSISTANT", apiResponse.getContent(), tokenCount, model.getId());

			// Emit the full response as a single SSE event
			SseEmitter emitter = new SseEmitter();
			try {
				emitter.send(SseEmitter.event()
						.name("token")
						.data("{\"content\":\"" + apiResponse.getContent().replace("\"", "\\\"") + "\",\"isThinking\":false}"));
				emitter.send(SseEmitter.event()
						.name("done")
						.data("{\"inputTokens\":" + apiResponse.getInputTokens() + ",\"outputTokens\":" + tokenCount + "}"));
			} catch (Exception e) {
				// Ignore — client likely disconnected
			}
			emitter.complete();
			return emitter;

		} catch (IllegalArgumentException e) {
			SseEmitter emitter = new SseEmitter();
			try {
				emitter.send(SseEmitter.event()
						.name("error")
						.data("{\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}"));
			} catch (Exception ex) {
				// Ignore
			}
			emitter.complete();
			return emitter;
		} catch (Exception e) {
			SseEmitter emitter = new SseEmitter();
			try {
				emitter.send(SseEmitter.event()
						.name("error")
						.data("{\"message\":\"Failed to get response: " + e.getMessage().replace("\"", "\\\"") + "\"}"));
			} catch (Exception ex) {
				// Ignore
			}
			emitter.complete();
			return emitter;
		}
	}

	private ConversationResponse toResponse(Conversation conversation) {
		Model model = conversation.getModel();
		ConversationResponse.ModelInfo modelInfo = ConversationResponse.ModelInfo.builder()
				.id(model != null ? model.getId() : null)
				.modelName(model != null ? model.getModelName() : null)
				.displayName(model != null ? model.getDisplayName() : null)
				.supportsVision(model != null ? model.getSupportsVision() : false)
				.build();

		return ConversationResponse.builder()
				.id(conversation.getId())
				.title(conversation.getTitle())
				.model(modelInfo)
				.updatedAt(conversation.getUpdatedAt())
				.build();
	}

	private MessageResponse toMessageResponse(Message message) {
		return MessageResponse.builder()
				.id(message.getId())
				.role(message.getRole() != null ? message.getRole().name() : null)
				.content(message.getContent())
				.tokenCount(message.getTokenCount())
				.createdAt(message.getCreatedAt())
				.build();
	}
}
