package com.company.specvalidator.dto.ai;

import com.company.specvalidator.enums.TargetAudience;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiValidationQuestion {
    private String question;
    private String reason;
    private TargetAudience targetAudience;
}
