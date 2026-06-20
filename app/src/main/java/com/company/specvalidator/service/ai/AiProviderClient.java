package com.company.specvalidator.service.ai;

import com.company.specvalidator.dto.ai.AiValidationRequest;
import com.company.specvalidator.dto.ai.AiValidationResponse;

public interface AiProviderClient {
    AiValidationResponse validateFunctionalSpecification(AiValidationRequest request);
}
