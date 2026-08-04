package com.company.specvalidator.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiValidationRequest {
    private Long documentId;
    private String systemPrompt;
    private String userPrompt;
    private String traceId;
    private Integer promptVersion;
}
