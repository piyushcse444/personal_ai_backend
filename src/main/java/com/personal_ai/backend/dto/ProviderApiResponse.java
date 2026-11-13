package com.personal_ai.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderApiResponse {

    private String content;
    private Integer inputTokens;
    private Integer outputTokens;
}
