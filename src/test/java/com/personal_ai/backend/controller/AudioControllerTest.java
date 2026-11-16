package com.personal_ai.backend.controller;

import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.model.Provider;
import com.personal_ai.backend.service.audio.AudioTranscriptionService;
import com.personal_ai.backend.service.model.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AudioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AudioTranscriptionService transcriptionService;

    @Mock
    private ModelService modelService;

    @InjectMocks
    private AudioController audioController;

    private UUID asrModelId;
    private UUID chatModelId;
    private Model asrModel;
    private Model chatModel;
    private Provider provider;

    @BeforeEach
    void setUp() {
        provider = Provider.builder()
                .id(UUID.randomUUID())
                .code("unsloth")
                .displayName("Unsloth Studio")
                .providerType(Provider.ProviderType.LOCAL)
                .build();

        asrModelId = UUID.randomUUID();
        asrModel = Model.builder()
                .id(asrModelId)
                .provider(provider)
                .modelName("qwen3-asr-0.6b")
                .displayName("Qwen3-ASR 0.6B")
                .contextWindow(4096)
                .isActive(true)
                .isOffline(true)
                .modelType(Model.ModelType.ASR)
                .build();

        chatModelId = UUID.randomUUID();
        chatModel = Model.builder()
                .id(chatModelId)
                .provider(provider)
                .modelName("test-chat-model")
                .displayName("Test Chat Model")
                .contextWindow(4096)
                .isActive(true)
                .modelType(Model.ModelType.CHAT)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(audioController).build();
    }

    @Test
    @DisplayName("Should transcribe audio successfully with valid ASR model")
    void transcribe_shouldSucceed_withValidAsrModel() throws Exception {
        // Arrange
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", "test.wav", "audio/wav", "fake-audio-data".getBytes());

        when(modelService.findById(asrModelId)).thenReturn(Optional.of(asrModel));
        when(transcriptionService.transcribe(any(byte[].class), anyString(), eq(asrModel)))
                .thenReturn("This is a mock transcription result.");

        // Act & Assert
        mockMvc.perform(multipart("/api/audio/transcribe")
                        .file(audioFile)
                        .param("modelId", asrModelId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.transcription").value("This is a mock transcription result."));
    }

    @Test
    @DisplayName("Should reject request with invalid model ID")
    void transcribe_shouldFail_withInvalidModelId() throws Exception {
        // Arrange
        UUID invalidId = UUID.randomUUID();
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", "test.wav", "audio/wav", "fake-audio-data".getBytes());

        when(modelService.findById(invalidId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(multipart("/api/audio/transcribe")
                        .file(audioFile)
                        .param("modelId", invalidId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Model not found: " + invalidId));
    }

    @Test
    @DisplayName("Should reject CHAT model for transcription")
    void transcribe_shouldFail_withChatModel() throws Exception {
        // Arrange
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", "test.wav", "audio/wav", "fake-audio-data".getBytes());

        when(modelService.findById(chatModelId)).thenReturn(Optional.of(chatModel));

        // Act & Assert
        mockMvc.perform(multipart("/api/audio/transcribe")
                        .file(audioFile)
                        .param("modelId", chatModelId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Selected model is not an ASR model. Model type: CHAT"));
    }

    @Test
    @DisplayName("Should reject empty audio file")
    void transcribe_shouldFail_withEmptyAudio() throws Exception {
        // Arrange
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", "empty.wav", "audio/wav", new byte[0]);

        // Act & Assert
        mockMvc.perform(multipart("/api/audio/transcribe")
                        .file(audioFile)
                        .param("modelId", asrModelId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Audio file is required and must not be empty"));
    }

    @Test
    @DisplayName("Should return 503 when Unsloth server is unavailable")
    void transcribe_shouldReturn503_whenServerUnavailable() throws Exception {
        // Arrange
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", "test.wav", "audio/wav", "fake-audio-data".getBytes());

        when(modelService.findById(asrModelId)).thenReturn(Optional.of(asrModel));
        doThrow(new IllegalStateException("Unsloth server is not healthy or unavailable"))
                .when(transcriptionService).transcribe(any(byte[].class), anyString(), any(Model.class));

        // Act & Assert
        mockMvc.perform(multipart("/api/audio/transcribe")
                        .file(audioFile)
                        .param("modelId", asrModelId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should return 429 when another transcription is in progress")
    void transcribe_shouldReturn429_whenBusy() throws Exception {
        // Arrange
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", "test.wav", "audio/wav", "fake-audio-data".getBytes());

        when(modelService.findById(asrModelId)).thenReturn(Optional.of(asrModel));
        doThrow(new IllegalStateException("Another transcription is in progress. Please try again later."))
                .when(transcriptionService).transcribe(any(byte[].class), anyString(), any(Model.class));

        // Act & Assert
        mockMvc.perform(multipart("/api/audio/transcribe")
                        .file(audioFile)
                        .param("modelId", asrModelId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should return 507 when GPU memory is insufficient")
    void transcribe_shouldReturn507_whenOutOfMemory() throws Exception {
        // Arrange
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", "test.wav", "audio/wav", "fake-audio-data".getBytes());

        when(modelService.findById(asrModelId)).thenReturn(Optional.of(asrModel));
        doThrow(new RuntimeException("CUDA out of memory"))
                .when(transcriptionService).transcribe(any(byte[].class), anyString(), any(Model.class));

        // Act & Assert
        mockMvc.perform(multipart("/api/audio/transcribe")
                        .file(audioFile)
                        .param("modelId", asrModelId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInsufficientStorage())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Insufficient GPU memory: CUDA out of memory"));
    }
}
