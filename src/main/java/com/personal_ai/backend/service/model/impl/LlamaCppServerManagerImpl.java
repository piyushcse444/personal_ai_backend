package com.personal_ai.backend.service.model.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.personal_ai.backend.service.model.LlamaCppServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "app.llamacpp.enabled", havingValue = "true")
public class LlamaCppServerManagerImpl implements LlamaCppServerManager {

	private static final Logger log = LoggerFactory.getLogger(LlamaCppServerManagerImpl.class);

	private final String baseUrl;
	private final String apiKey;
	private final String defaultModel;
	private final int maxSeqLength;
	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	// Tracks in-progress model loads to coalesce concurrent requests.
	// Key: model path, Value: CompletableFuture that completes when loading finishes.
	private final ConcurrentHashMap<String, CompletableFuture<Boolean>> loadingFutures = new ConcurrentHashMap<>();

	// Dedicated executor for model loading — prevents blocking the common ForkJoinPool
	// which can cause HikariCP thread starvation warnings.
	private static final ExecutorService loadExecutor = Executors.newSingleThreadExecutor(
			r -> new Thread(r, "unsloth-model-loader"));

	public LlamaCppServerManagerImpl(
			@org.springframework.beans.factory.annotation.Value("${app.llamacpp.base-url:http://127.0.0.1:8888}") String baseUrl,
			@org.springframework.beans.factory.annotation.Value("${app.llamacpp.api-key:sk-unsloth-4203b2881dbe605f5363a3a333f63a22}") String apiKey,
			@org.springframework.beans.factory.annotation.Value("${app.llamacpp.model:AtomicChat/LFM3-1.7B-Uncensored-GGUF}") String defaultModel,
			@org.springframework.beans.factory.annotation.Value("${app.llamacpp.max-seq-length:2048}") int maxSeqLength) {
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
		this.defaultModel = defaultModel;
		this.maxSeqLength = maxSeqLength;
		this.objectMapper = new ObjectMapper();
		this.restClient = RestClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.build();
	}

	@PostConstruct
	public void init() {
		log.info("LlamaCppServerManager initialized: baseUrl={}, model={}", baseUrl, defaultModel);
	}

	@Override
	public boolean checkHealth() {
		try {
			String response = restClient.get()
					.uri("/api/health")
					.retrieve()
					.body(String.class);
			if (response != null) {
				JsonNode json = objectMapper.readTree(response);
				String status = json.path("status").asText("");
				boolean healthy = "healthy".equals(status);
				if (healthy) {
					log.debug("Unsloth API health check passed");
				} else {
					log.warn("Unsloth API health check returned status: {}", status);
				}
				return healthy;
			}
			return false;
		} catch (Exception e) {
			log.warn("Unsloth API health check failed: {}", e.getMessage());
			return false;
		}
	}

	@Override
	public List<ModelInfo> listModels() {
		try {
			String response = restClient.get()
					.uri("/v1/models")
					.retrieve()
					.body(String.class);
			if (response == null) {
				return Collections.emptyList();
			}
			JsonNode json = objectMapper.readTree(response);
			JsonNode data = json.path("data");
			List<ModelInfo> models = new ArrayList<>();
			if (data.isArray()) {
				for (JsonNode node : data) {
					String id = node.path("id").asText("");
					String object = node.path("object").asText("");
					long created = node.path("created").asLong(0);
					String ownedBy = node.path("owned_by").asText("");
					boolean loaded = node.path("loaded").asBoolean(false);
					String quant = node.path("quant").asText("");
					String displayName = node.path("display_name").asText("");
					String statusValue = node.path("status").path("value").asText("");
					models.add(new ModelInfo(id, object, created, ownedBy, loaded, quant, displayName, statusValue));
				}
			}
			log.debug("Listed {} models from Unsloth API", models.size());
			return models;
		} catch (Exception e) {
			log.error("Failed to list models from Unsloth API: {}", e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public InferenceStatus getInferenceStatus() {
		try {
			String response = restClient.get()
					.uri("/api/inference/status")
					.retrieve()
					.body(String.class);
			if (response == null) {
				return null;
			}
			JsonNode json = objectMapper.readTree(response);
			String activeModel = json.path("active_model").asText("");
			String modelIdentifier = json.path("model_identifier").asText("");
			int contextLength = json.path("context_length").asInt(0);
			int maxContextLength = json.path("max_context_length").asInt(0);
			boolean supportsReasoning = json.path("supports_reasoning").asBoolean(false);
			boolean supportsTools = json.path("supports_tools").asBoolean(false);
			String ggufVariant = json.path("gguf_variant").asText("");

			List<String> loaded = new ArrayList<>();
			JsonNode loadedNode = json.path("loaded");
			if (loadedNode.isArray()) {
				for (JsonNode node : loadedNode) {
					loaded.add(node.asText());
				}
			}

			List<String> serving = new ArrayList<>();
			JsonNode servingNode = json.path("serving");
			if (servingNode.isArray()) {
				for (JsonNode node : servingNode) {
					serving.add(node.asText());
				}
			}

			log.debug("Inference status: activeModel={}, loaded={}", activeModel, loaded);
			return new InferenceStatus(activeModel, modelIdentifier, loaded, serving,
					contextLength, maxContextLength, supportsReasoning, supportsTools, ggufVariant);
		} catch (Exception e) {
			log.warn("Failed to get inference status: {}", e.getMessage());
			return null;
		}
	}

	@Override
	public boolean loadModel(String modelPath, int seqLength) {
		try {
			log.info("Loading model: {} with max_seq_length: {}", modelPath, seqLength);
			ObjectNode body = objectMapper.createObjectNode();
			body.put("model_path", modelPath);
			body.put("is_lora", false);
			body.put("max_seq_length", seqLength);

			String response = restClient.post()
					.uri("/api/inference/load")
					.contentType(MediaType.APPLICATION_JSON)
					.body(body.toString())
					.retrieve()
					.body(String.class);

			if (response == null) {
				log.error("Model load returned null response");
				return false;
			}

			JsonNode json = objectMapper.readTree(response);
			String status = json.path("status").asText("");
			boolean success = "loaded".equals(status);
			if (success) {
				log.info("Model {} loaded successfully", modelPath);
			} else {
				log.warn("Model load returned status: {}", status);
			}
			return success;
		} catch (Exception e) {
			log.error("Failed to load model {}: {}", modelPath, e.getMessage(), e);
			return false;
		}
	}

	@Override
	public boolean unloadModel(String modelPath) {
		try {
			log.info("Unloading model: {}", modelPath);
			ObjectNode body = objectMapper.createObjectNode();
			body.put("model_path", modelPath);
			body.put("is_lora", false);
			body.put("max_seq_length", maxSeqLength);

			String response = restClient.post()
					.uri("/api/inference/unload")
					.contentType(MediaType.APPLICATION_JSON)
					.body(body.toString())
					.retrieve()
					.body(String.class);

			if (response == null) {
				log.error("Model unload returned null response");
				return false;
			}

			JsonNode json = objectMapper.readTree(response);
			String status = json.path("status").asText("");
			boolean success = "unloaded".equals(status);
			if (success) {
				log.info("Model {} unloaded successfully", modelPath);
			} else {
				log.warn("Model unload returned status: {}", status);
			}
			return success;
		} catch (Exception e) {
			log.error("Failed to unload model {}: {}", modelPath, e.getMessage(), e);
			return false;
		}
	}

	@Override
	public boolean isModelLoaded(String modelPath) {
		InferenceStatus status = getInferenceStatus();
		if (status == null) {
			return false;
		}
		return status.loaded().contains(modelPath) || modelPath.equals(status.activeModel());
	}

	/**
	 * Verify that a model is not just loaded but actively serving.
	 * This is a stricter check than isModelLoaded().
	 */
	private boolean verifyModelServing(String modelPath) {
		InferenceStatus status = getInferenceStatus();
		if (status == null) {
			log.warn("Could not verify model serving: inference status unavailable");
			return false;
		}
		boolean isServing = status.serving().contains(modelPath) || modelPath.equals(status.activeModel());
		if (!isServing) {
			log.warn("Model {} is loaded but not actively serving. Loaded: {}, Serving: {}",
					modelPath, status.loaded(), status.serving());
		}
		return isServing;
	}

	/**
	 * Ensure the model is ready for inference, following this flow:
	 * 1. Check Unsloth service health (not model readiness)
	 * 2. Check expected model exists in available models
	 * 3. Check whether model is already loaded
	 * 4. If loaded, verify it is serving
	 * 5. If not loaded, load it (with concurrency control) and verify
	 */
	@Override
	public boolean ensureModelReady(String modelPath) {
		// Step 1: Check Unsloth service health — this does NOT mean the model is ready.
		log.debug("Checking Unsloth service health...");
		if (!checkHealth()) {
			log.error("Unsloth API service is not healthy. Cannot ensure model readiness.");
			return false;
		}
		log.debug("Unsloth service is healthy — proceeding to model checks.");

		// Step 2: Check if model exists in the available models list
		List<ModelInfo> models = listModels();
		boolean modelExists = models.stream()
				.anyMatch(m -> m.id().equals(modelPath) || m.id().startsWith(modelPath));

		if (!modelExists) {
			log.error("Model {} not found in available models list. Cannot load a non-existent model.", modelPath);
			return false;
		}
		log.debug("Model {} exists in available models.", modelPath);

		// Step 3: Check whether model is already loaded and serving
		if (isModelLoaded(modelPath) && verifyModelServing(modelPath)) {
			log.debug("Model {} is already loaded and serving.", modelPath);
			return true;
		}

		// Step 4: Model is not loaded (or not serving) — initiate load with concurrency control.
		// Use compute to ensure only one thread creates the load task; others will wait on the same future.
		log.info("Model {} is not ready. Initiating load with concurrency control...", modelPath);
		CompletableFuture<Boolean> future = loadingFutures.compute(modelPath, (key, existingFuture) -> {
			if (existingFuture != null && !existingFuture.isDone()) {
				log.info("Model {} is already being loaded by another request. Waiting for it to complete.", modelPath);
				return existingFuture;
			}
			// Either no existing future or it's already done — create a new one.
			// Use dedicated executor to avoid blocking the common ForkJoinPool.
			return CompletableFuture.supplyAsync(() -> {
				try {
					log.info("Loading model: {} (exclusive load operation)", modelPath);
					boolean loaded = loadModel(modelPath, maxSeqLength);

					if (!loaded) {
						log.error("Failed to load model {}", modelPath);
						return false;
					}

					// Step 5: Wait and verify the model is loaded and serving.
					// Use a polling approach with timeout instead of blind sleep.
					log.info("Model load API returned success. Verifying model is actually serving...");
					int maxAttempts = 30; // up to 30 seconds
					int attempt = 0;
					while (attempt < maxAttempts) {
						try {
							TimeUnit.SECONDS.sleep(1);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							log.warn("Interrupted while waiting for model to serve");
							return false;
						}
						attempt++;

						if (isModelLoaded(modelPath)) {
							log.debug("Model {} is loaded after {} seconds.", modelPath, attempt);
							if (verifyModelServing(modelPath)) {
								log.info("Model {} is loaded and serving after {} seconds.", modelPath, attempt);
								return true;
							}
							log.debug("Model {} is loaded but not yet serving. Retrying... (attempt {}/{})",
									modelPath, attempt, maxAttempts);
						}
					}

					log.error("Model {} loaded but never started serving within timeout ({} attempts).",
							modelPath, maxAttempts);
					return false;

				} finally {
					// Clean up the future from the map so future loads can happen (e.g. after unload).
					loadingFutures.remove(modelPath);
				}
			}, loadExecutor);
		});

		// Wait for the load to complete and return the result.
		try {
			boolean ready = future.get(60, TimeUnit.SECONDS);
			if (ready) {
				log.info("Model {} is ready for inference.", modelPath);
			} else {
				log.error("Model {} failed to become ready for inference.", modelPath);
			}
			return ready;
		} catch (Exception e) {
			log.error("Timeout or error waiting for model {} to load: {}", modelPath, e.getMessage());
			return false;
		}
	}
}
