package com.personal_ai.backend.service.model;

import java.util.List;

/**
 * Manages interaction with the Unsloth Studio API for model inference.
 * Replaces the previous local llama.cpp process management with API-based model control.
 */
public interface LlamaCppServerManager {

	/**
	 * Check if the Unsloth API server is healthy and responsive.
	 *
	 * @return true if the health endpoint responds successfully
	 */
	boolean checkHealth();

	/**
	 * List available models from the Unsloth API.
	 *
	 * @return list of model information
	 */
	List<ModelInfo> listModels();

	/**
	 * Get the current inference status (loaded models, engine info, etc.).
	 *
	 * @return the inference status response, or null if not available
	 */
	InferenceStatus getInferenceStatus();

	/**
	 * Load a model into the inference engine.
	 *
	 * @param modelPath the model path/identifier
	 * @param maxSeqLength the maximum sequence length
	 * @return true if the model was loaded successfully
	 */
	boolean loadModel(String modelPath, int maxSeqLength);

	/**
	 * Unload a model from the inference engine.
	 *
	 * @param modelPath the model path/identifier
	 * @return true if the model was unloaded successfully
	 */
	boolean unloadModel(String modelPath);

	/**
	 * Check if a specific model is currently loaded and ready for inference.
	 *
	 * @param modelPath the model path/identifier
	 * @return true if the model is loaded
	 */
	boolean isModelLoaded(String modelPath);

	/**
	 * Ensure the model is loaded. Checks health, then loads if not already loaded.
	 *
	 * @param modelPath the model path/identifier
	 * @return true if the model is ready for inference
	 */
	boolean ensureModelReady(String modelPath);

	/**
	 * Information about an available model.
	 */
	record ModelInfo(
			String id,
			String object,
			long created,
			String ownedBy,
			boolean loaded,
			String quant,
			String displayName,
			String statusValue
	) {}

	/**
	 * Inference status response from the API.
	 */
	record InferenceStatus(
			String activeModel,
			String modelIdentifier,
			List<String> loaded,
			List<String> serving,
			int contextLength,
			int maxContextLength,
			boolean supportsReasoning,
			boolean supportsTools,
			String ggufVariant
		) {}
}
