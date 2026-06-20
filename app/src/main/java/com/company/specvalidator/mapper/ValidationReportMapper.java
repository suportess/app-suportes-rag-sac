package com.company.specvalidator.mapper;

import com.company.specvalidator.dto.response.ValidationIssueResponse;
import com.company.specvalidator.dto.response.ValidationQuestionResponse;
import com.company.specvalidator.dto.response.ValidationReportResponse;
import com.company.specvalidator.entity.ValidationIssueEntity;
import com.company.specvalidator.entity.ValidationQuestionEntity;
import com.company.specvalidator.entity.ValidationReportEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidationReportMapper {

    public ValidationReportResponse toResponse(
            ValidationReportEntity report,
            List<ValidationIssueEntity> issues,
            List<ValidationQuestionEntity> questions,
            List<String> positivePoints,
            List<String> missingSections,
            String riskAnalysis) {

        return ValidationReportResponse.builder()
                .reportId(report.getId())
                .documentId(report.getDocument().getId())
                .status(report.getStatus())
                .score(report.getScore())
                .specificationSummary(report.getSpecificationSummary())
                .summary(report.getSummary())
                .finalRecommendation(report.getFinalRecommendation())
                .issues(issues.stream().map(this::toIssueResponse).toList())
                .questions(questions.stream().map(this::toQuestionResponse).toList())
                .positivePoints(positivePoints)
                .missingSections(missingSections)
                .riskAnalysis(riskAnalysis)
                .build();
    }

    private ValidationIssueResponse toIssueResponse(ValidationIssueEntity entity) {
        return ValidationIssueResponse.builder()
                .severity(entity.getSeverity())
                .category(entity.getCategory())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .suggestion(entity.getSuggestion())
                .build();
    }

    private ValidationQuestionResponse toQuestionResponse(ValidationQuestionEntity entity) {
        return ValidationQuestionResponse.builder()
                .question(entity.getQuestion())
                .reason(entity.getReason())
                .targetAudience(entity.getTargetAudience())
                .build();
    }
}
