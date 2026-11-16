package com.personal_ai.backend.controller;

import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.model.Provider;
import com.personal_ai.backend.service.model.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ModelControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ModelService modelService;

    @InjectMocks
    private ModelController modelController;

    private Provider provider;
    private Model asrModel;
    private Model chatModel;

    @BeforeEach
    void setUp() {
        provider = Provider.builder()
                .id(UUID.randomUUID())
                .code("unsloth")
                .displayName("Unsloth Studio")
                .providerType(Provider.ProviderType.LOCAL)
                .build();

        asrModel = Model.builder()
                .id(UUID.randomUUID())
                .provider(provider)
                .modelName("qwen3-asr-0.6b")
                .displayName("Qwen3-ASR 0.6B")
                .contextWindow(4096)
                .isActive(true)
                .isOffline(true)
                .modelType(Model.ModelType.ASR)
                .build();

        chatModel = Model.builder()
                .id(UUID.randomUUID())
                .provider(provider)
                .modelName("test-chat-model")
                .displayName("Test Chat Model")
                .contextWindow(4096)
                .isActive(true)
                .modelType(Model.ModelType.CHAT)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(modelController).build();
    }

    @Test
    @DisplayName("Should return ASR models when type=ASR")
    void listModels_shouldReturnAsrModels_whenTypeAsr() throws Exception {
        // Arrange
        when(modelService.findActiveByModelType(Model.ModelType.ASR))
                .thenReturn(Arrays.asList(asrModel));

        // Act & Assert
        mockMvc.perform(get("/api/models").param("type", "ASR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modelType").value("ASR"))
                .andExpect(jsonPath("$[0].displayName").value("Qwen3-ASR 0.6B"))
                .andExpect(jsonPath("$[0].modelName").value("qwen3-asr-0.6b"));
    }

    @Test
    @DisplayName("Should return CHAT models when type=CHAT")
    void listModels_shouldReturnChatModels_whenTypeChat() throws Exception {
        // Arrange
        when(modelService.findActiveByModelType(Model.ModelType.CHAT))
                .thenReturn(Arrays.asList(chatModel));

        // Act & Assert
        mockMvc.perform(get("/api/models").param("type", "CHAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modelType").value("CHAT"));
    }

    @Test
    @DisplayName("Should return all models when no type filter")
    void listModels_shouldReturnAllModels_whenNoType() throws Exception {
        // Arrange
        when(modelService.findAll())
                .thenReturn(Arrays.asList(asrModel, chatModel));

        // Act & Assert
        mockMvc.perform(get("/api/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should return empty list when no ASR models exist")
    void listModels_shouldReturnEmpty_whenNoAsrModels() throws Exception {
        // Arrange
        when(modelService.findActiveByModelType(Model.ModelType.ASR))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/models").param("type", "ASR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should return 400 for invalid model type")
    void listModels_shouldReturn400_whenInvalidType() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/models").param("type", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should include provider information in response")
    void listModels_shouldIncludeProviderInfo() throws Exception {
        // Arrange
        when(modelService.findActiveByModelType(Model.ModelType.ASR))
                .thenReturn(Arrays.asList(asrModel));

        // Act & Assert
        mockMvc.perform(get("/api/models").param("type", "ASR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerId").exists())
                .andExpect(jsonPath("$[0].providerCode").value("unsloth"))
                .andExpect(jsonPath("$[0].providerDisplayName").value("Unsloth Studio"));
    }

    @Test
    @DisplayName("Should handle case-insensitive type parameter")
    void listModels_shouldHandleCaseInsensitiveType() throws Exception {
        // Arrange
        when(modelService.findActiveByModelType(Model.ModelType.ASR))
                .thenReturn(Arrays.asList(asrModel));

        // Act & Assert
        mockMvc.perform(get("/api/models").param("type", "asr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modelType").value("ASR"));
    }
}
