package com.company.specvalidator.dto.ai;

import com.company.specvalidator.enums.IssueSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiValidationIssue {
    private IssueSeverity severity;
    private String category;
    private String title;
    private String description;
    private String suggestion;
}
