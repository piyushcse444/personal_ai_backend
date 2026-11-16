package com.personal_ai.backend.dto;

import com.personal_ai.backend.model.Model;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ModelResponse {

    private UUID id;
    private String modelName;
    private String displayName;
    private Integer contextWindow;
    private Boolean isActive;
    private Boolean isOffline;
    private Boolean supportsVision;
    private Model.ModelType modelType;
    private UUID providerId;
    private String providerCode;
    private String providerDisplayName;

    public static ModelResponse from(Model model) {
        return ModelResponse.builder()
                .id(model.getId())
                .modelName(model.getModelName())
                .displayName(model.getDisplayName())
                .contextWindow(model.getContextWindow())
                .isActive(model.getIsActive())
                .isOffline(model.getIsOffline())
                .supportsVision(model.getSupportsVision())
                .modelType(model.getModelType())
                .providerId(model.getProvider().getId())
                .providerCode(model.getProvider().getCode())
                .providerDisplayName(model.getProvider().getDisplayName())
                .build();
    }
}
