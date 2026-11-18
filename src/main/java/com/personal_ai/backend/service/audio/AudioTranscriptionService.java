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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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

        boolean modelWasLoaded = false;
        try {
            // Step 1: Check Unsloth health
            log.info("Starting transcription with model: {}", modelPath);
            if (!serverManager.checkHealth()) {
                throw new IllegalStateException("Unsloth server is not healthy or unavailable");
            }

            // Step 2: Perform transcription
            String transcription = performTranscription(audioBytes, audioFilename, modelPath);
            log.info("Transcription completed successfully, length: {} chars", transcription.length());
            return transcription;

        } catch (Exception e) {
            log.error("Transcription failed: {}", e.getMessage(), e);
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        }
    }

    /**
     * Perform the actual transcription request to Unsloth.
     * Builds the multipart body manually as bytes so Content-Length is set explicitly,
     * because Unsloth's server rejects chunked transfer encoding (HTTP 411).
     */
    private String performTranscription(byte[] audioBytes, String audioFilename, String modelPath) {
        try {
            // Determine content type from filename
            String fileContentType = detectContentType(audioFilename);
            String safeFilename = audioFilename != null ? audioFilename : "audio.wav";

            // Build multipart body manually so we can compute Content-Length
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            // Write "file" part
            String fileHeader = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFilename + "\"\r\n"
                    + "Content-Type: " + fileContentType + "\r\n"
                    + "\r\n";
            bos.write(fileHeader.getBytes(StandardCharsets.UTF_8));
            bos.write(audioBytes);
            bos.write("\r\n".getBytes(StandardCharsets.UTF_8));

            // Write "model" part
            String modelHeader = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"model\"\r\n"
                    + "Content-Type: text/plain\r\n"
                    + "\r\n";
            bos.write(modelHeader.getBytes(StandardCharsets.UTF_8));
            bos.write(modelPath.getBytes(StandardCharsets.UTF_8));
            bos.write("\r\n".getBytes(StandardCharsets.UTF_8));

            // Write closing boundary
            bos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            bos.flush();

            byte[] multipartBody = bos.toByteArray();

            log.info("Sending transcription request to Unsloth: model={}, file={}, bodySize={} bytes",
                    modelPath, safeFilename, multipartBody.length);

            // Send the request with explicit Content-Length
            String response = restClient.post()
                    .uri("/v1/audio/transcriptions")
                    .contentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(multipartBody.length))
                    .body(new ByteArrayResource(multipartBody))
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
