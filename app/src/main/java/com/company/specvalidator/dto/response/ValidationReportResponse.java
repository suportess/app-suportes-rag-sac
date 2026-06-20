package com.company.specvalidator.dto.response;

import com.company.specvalidator.enums.ValidationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationReportResponse {
    private Long reportId;
    private Long documentId;
    private ValidationStatus status;
    private Integer score;
    private String specificationSummary;
    private String summary;
    private String finalRecommendation;
    private List<ValidationIssueResponse> issues;
    private List<ValidationQuestionResponse> questions;
    private List<String> positivePoints;
    private List<String> missingSections;
    private String riskAnalysis;
}
