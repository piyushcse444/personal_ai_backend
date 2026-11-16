package com.personal_ai.backend.service.audio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.service.model.UnslothServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for audio transcription using Unsloth ASR models.
 * Handles model loading, transcription, and unloading while preserving any loaded CHAT model.
 */
@Service
@ConditionalOnBean(UnslothServerManager.class)
public class AudioTranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(AudioTranscriptionService.class);

    private final UnslothServerManager serverManager;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;

    // Lock to prevent concurrent ASR model load/unload operations
    private final ReentrantLock asrLock = new ReentrantLock();

    // Track which ASR model is currently loaded to avoid redundant loads
    private volatile String currentlyLoadedAsrModel = null;

    public AudioTranscriptionService(
            UnslothServerManager serverManager,
            @org.springframework.beans.factory.annotation.Value("${app.unsloth.base-url:http://127.0.0.1:8888}") String baseUrl,
            @org.springframework.beans.factory.annotation.Value("${app.unsloth.api-key:sk-unsloth-4203b2881dbe605f5363a3a333f63a22}") String apiKey) {
        this.serverManager = serverManager;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.simple().build();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    /**
     * Transcribe audio using the specified ASR model.
     * This method handles the full lifecycle: load model -> transcribe -> unload model.
     *
     * @param audioBytes the audio file bytes
     * @param audioFilename the original filename (used to determine content type)
     * @param model the ASR model to use
     * @return the transcribed text
     */
    public String transcribe(byte[] audioBytes, String audioFilename, Model model) {
        String modelPath = model.getModelName();

        if (!asrLock.tryLock()) {
            throw new IllegalStateException("Another transcription is in progress. Please try again later.");
        }

        boolean modelWasLoaded = false;
        try {
            // Step 1: Check Unsloth health
            log.info("Starting transcription with model: {}", modelPath);
            if (!serverManager.checkHealth()) {
                throw new IllegalStateException("Unsloth server is not healthy or unavailable");
            }

            // Step 2: Check if ASR model is already loaded
            boolean alreadyLoaded = serverManager.isModelLoaded(modelPath);
            if (alreadyLoaded) {
                log.info("ASR model {} is already loaded", modelPath);
                modelWasLoaded = true;
                currentlyLoadedAsrModel = modelPath;
            } else {
                // Step 3: Load the ASR model
                log.info("Loading ASR model: {}", modelPath);
                if (!serverManager.ensureModelReady(modelPath)) {
                    throw new IllegalStateException("Failed to load ASR model: " + modelPath);
                }
                modelWasLoaded = true;
                currentlyLoadedAsrModel = modelPath;
                log.info("ASR model {} loaded successfully", modelPath);
            }

            // Step 4: Perform transcription
            String transcription = performTranscription(audioBytes, audioFilename, modelPath);
            log.info("Transcription completed successfully, length: {} chars", transcription.length());
            return transcription;

        } catch (Exception e) {
            log.error("Transcription failed: {}", e.getMessage(), e);
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        } finally {
            // Step 5: Unload the ASR model
            try {
                unloadAsrModel(modelPath);
            } finally {
                asrLock.unlock();
            }
        }
    }

    /**
     * Perform the actual transcription request to Unsloth.
     */
    private String performTranscription(byte[] audioBytes, String audioFilename, String modelPath) {
        try {
            // Determine content type from filename
            String contentType = detectContentType(audioFilename);

            // Build multipart request body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return audioFilename != null ? audioFilename : "audio.wav";
                }
            };
            body.add("file", fileResource);
            body.add("model", modelPath);

            log.info("Sending transcription request to Unsloth: model={}, file={}, contentType={}",
                    modelPath, audioFilename, contentType);

            // Send the request
            String response = restClient.post()
                    .uri("/v1/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isEmpty()) {
                throw new IllegalStateException("Unsloth ASR returned an empty response");
            }

            // Parse the response - OpenAI-compatible format with "text" field
            JsonNode json = objectMapper.readTree(response);
            String text = json.path("text").asText("");

            if (text.isEmpty()) {
                log.warn("ASR returned empty transcription. Raw response: {}", response);
                throw new IllegalStateException("ASR model returned an empty transcription");
            }

            return text.trim();

        } catch (Exception e) {
            log.error("ASR inference failed: {}", e.getMessage(), e);
            throw new RuntimeException("ASR inference failed: " + e.getMessage(), e);
        }
    }

    /**
     * Unload the ASR model after transcription.
     * Only unloads if we loaded it or it was loaded for ASR purposes.
     */
    private void unloadAsrModel(String modelPath) {
        if (currentlyLoadedAsrModel != null && currentlyLoadedAsrModel.equals(modelPath)) {
            try {
                log.info("Unloading ASR model: {}", modelPath);
                serverManager.unloadModel(modelPath);
                log.info("ASR model {} unloaded successfully", modelPath);
            } catch (Exception e) {
                log.warn("Failed to unload ASR model {}: {}", modelPath, e.getMessage());
            } finally {
                currentlyLoadedAsrModel = null;
            }
        }
    }

    /**
     * Detect content type from filename extension.
     */
    private String detectContentType(String filename) {
        if (filename == null) {
            return "audio/wav";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".flac")) return "audio/flac";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".webm")) return "audio/webm";
        if (lower.endsWith(".amr")) return "audio/amr";
        return "audio/wav";
    }
}
