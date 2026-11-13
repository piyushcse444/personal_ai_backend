package com.personal_ai.backend.controller;

import com.personal_ai.backend.dto.*;
import com.personal_ai.backend.model.Conversation;
import com.personal_ai.backend.model.Message;
import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.model.Provider;
import com.personal_ai.backend.model.ProviderConfig;
import com.personal_ai.backend.service.ConversationService;
import com.personal_ai.backend.service.MessageService;
import com.personal_ai.backend.service.ProviderApiService;
import com.personal_ai.backend.service.ProviderApiServiceFactory;
import com.personal_ai.backend.service.ProviderConfigService;
import com.personal_ai.backend.repository.ConversationRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

	private final ConversationService conversationService;
	private final MessageService messageService;
	private final ProviderConfigService providerConfigService;
	private final ProviderApiServiceFactory providerApiServiceFactory;
	private final ConversationRepository conversationRepository;

	public ConversationController(ConversationService conversationService, MessageService messageService,
								ProviderConfigService providerConfigService,
								ProviderApiServiceFactory providerApiServiceFactory,
								ConversationRepository conversationRepository) {
		this.conversationService = conversationService;
		this.messageService = messageService;
		this.providerConfigService = providerConfigService;
		this.providerApiServiceFactory = providerApiServiceFactory;
		this.conversationRepository = conversationRepository;
	}

	@PostMapping
	public ResponseEntity<?> createConversation(@Valid @RequestBody CreateConversationRequest request) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);

			// Get the provider config to find the provider ID
			var configOpt = providerConfigService.findById(request.getProviderConfigId());
			UUID providerId = configOpt.map(c -> c.getProvider().getId()).orElse(null);

			Conversation conversation = conversationService.createConversation(
					userId,
					providerId,
					request.getModelId(),
					request.getProviderConfigId(),
					request.getTitle()
			);

			return ResponseEntity.status(201).body(toResponse(conversation));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@GetMapping
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
			ProviderApiResponse apiResponse = apiService.chat(history, model, providerConfig);

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

	private ConversationResponse toResponse(Conversation conversation) {
		Model model = conversation.getModel();
		ConversationResponse.ModelInfo modelInfo = ConversationResponse.ModelInfo.builder()
				.id(model != null ? model.getId() : null)
				.modelName(model != null ? model.getModelName() : null)
				.displayName(model != null ? model.getDisplayName() : null)
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
