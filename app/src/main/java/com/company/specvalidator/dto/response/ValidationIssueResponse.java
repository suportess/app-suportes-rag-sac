package com.company.specvalidator.dto.response;

import com.company.specvalidator.enums.IssueSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationIssueResponse {
    private IssueSeverity severity;
    private String category;
    private String title;
    private String description;
    private String suggestion;
}
