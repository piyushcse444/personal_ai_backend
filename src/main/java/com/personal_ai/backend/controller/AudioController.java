package com.personal_ai.backend.controller;

import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.model.Model.ModelType;
import com.personal_ai.backend.service.audio.AudioTranscriptionService;
import com.personal_ai.backend.service.model.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/audio")
@ConditionalOnBean(AudioTranscriptionService.class)
@Tag(name = "Audio", description = "Audio transcription APIs")
public class AudioController {

    private final AudioTranscriptionService transcriptionService;
    private final ModelService modelService;

    public AudioController(AudioTranscriptionService transcriptionService, ModelService modelService) {
        this.transcriptionService = transcriptionService;
        this.modelService = modelService;
    }

    @PostMapping("/transcribe")
    @Operation(summary = "Transcribe audio", description = "Transcribe an audio file to text using the specified ASR model")
    public ResponseEntity<?> transcribe(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("modelId") UUID modelId) {
        try {
            // Validate audio file
            if (audio == null || audio.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Audio file is required and must not be empty"
                ));
            }

            // Validate model ID
            Model model = modelService.findById(modelId)
                    .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

            // Validate model type is ASR
            if (model.getModelType() != ModelType.ASR) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Selected model is not an ASR model. Model type: " + model.getModelType()
                ));
            }

            // Validate model is active
            if (!model.getIsActive()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Selected model is not active"
                ));
            }

            // Perform transcription
            byte[] audioBytes = audio.getBytes();
            String filename = audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.wav";
            String transcription = transcriptionService.transcribe(audioBytes, filename, model);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "transcription", transcription
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            if (message != null && message.contains("Another transcription is in progress")) {
                return ResponseEntity.status(429).body(Map.of(
                        "success", false,
                        "message", message
                ));
            }
            if (message != null && (message.contains("Unsloth server") || message.contains("not healthy"))) {
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", message
                ));
            }
            if (message != null && (message.contains("Failed to load ASR") || message.contains("not ready"))) {
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", message
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", message != null ? message : "Transcription failed"
            ));
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null && message.contains("CUDA")) {
                return ResponseEntity.status(507).body(Map.of(
                        "success", false,
                        "message", "Insufficient GPU memory: " + message
                ));
            }
            if (message != null && message.contains("ASR inference failed")) {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "message", message
                ));
            }
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Transcription failed: " + (message != null ? message : "Unknown error")
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Unexpected error during transcription: " + e.getMessage()
            ));
        }
    }
}
