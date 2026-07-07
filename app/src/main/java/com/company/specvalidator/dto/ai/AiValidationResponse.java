package com.company.specvalidator.dto.ai;

import com.company.specvalidator.dto.response.SectionStatus;
import com.company.specvalidator.enums.ValidationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiValidationResponse {
    private ValidationStatus status;
    private Integer score;
    private String specificationSummary;
    private String summary;
    private String finalRecommendation;
    @Builder.Default
    private List<AiValidationIssue> issues = new ArrayList<>();
    @Builder.Default
    private List<AiValidationQuestion> questions = new ArrayList<>();
    @Builder.Default
    private List<String> positivePoints = new ArrayList<>();
    @Builder.Default
    private List<String> missingSections = new ArrayList<>();
    private String riskAnalysis;
    @Builder.Default
    private List<SectionStatus> sectionAnalysis = new ArrayList<>();
}
