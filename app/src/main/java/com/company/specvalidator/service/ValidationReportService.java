package com.company.specvalidator.service;

import com.company.specvalidator.dto.ai.AiValidationIssue;
import com.company.specvalidator.dto.ai.AiValidationQuestion;
import com.company.specvalidator.dto.ai.AiValidationResponse;
import com.company.specvalidator.entity.DocumentEntity;
import com.company.specvalidator.entity.ValidationIssueEntity;
import com.company.specvalidator.entity.ValidationQuestionEntity;
import com.company.specvalidator.entity.ValidationReportEntity;
import com.company.specvalidator.enums.IssueSeverity;
import com.company.specvalidator.exception.ResourceValidationException;
import com.company.specvalidator.repository.ValidationIssueRepository;
import com.company.specvalidator.repository.ValidationQuestionRepository;
import com.company.specvalidator.repository.ValidationReportRepository;
import com.company.specvalidator.dto.response.SectionStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ValidationReportService {

    private final ValidationReportRepository validationReportRepository;
    private final ValidationIssueRepository validationIssueRepository;
    private final ValidationQuestionRepository validationQuestionRepository;
    private final ObjectMapper objectMapper;

    public ValidationReportService(ValidationReportRepository validationReportRepository,
                                   ValidationIssueRepository validationIssueRepository,
                                   ValidationQuestionRepository validationQuestionRepository,
                                   ObjectMapper objectMapper) {
        this.validationReportRepository = validationReportRepository;
        this.validationIssueRepository = validationIssueRepository;
        this.validationQuestionRepository = validationQuestionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ValidationReportEntity saveReport(DocumentEntity document, AiValidationResponse response) {
        int critical = (int) response.getIssues().stream().filter(i -> i.getSeverity() == IssueSeverity.CRITICAL).count();
        int moderate = (int) response.getIssues().stream().filter(i -> i.getSeverity() == IssueSeverity.MODERATE).count();
        int minor = (int) response.getIssues().stream().filter(i -> i.getSeverity() == IssueSeverity.MINOR).count();

        ValidationReportEntity report = ValidationReportEntity.builder()
                .document(document)
                .status(response.getStatus())
                .score(response.getScore())
                .specificationSummary(response.getSpecificationSummary())
                .summary(response.getSummary())
                .finalRecommendation(response.getFinalRecommendation())
            .positivePointsJson(toJson(response.getPositivePoints()))
            .missingSectionsJson(toJson(response.getMissingSections()))
            .sectionAnalysisJson(toJsonObject(response.getSectionAnalysis()))
            .riskAnalysis(response.getRiskAnalysis() == null ? "" : response.getRiskAnalysis())
                .criticalIssuesCount(critical)
                .moderateIssuesCount(moderate)
                .minorIssuesCount(minor)
                .build();

        ValidationReportEntity saved = validationReportRepository.save(report);

        List<ValidationIssueEntity> issueEntities = response.getIssues().stream()
                .map(issue -> toIssueEntity(saved, issue))
                .toList();
        validationIssueRepository.saveAll(issueEntities);

        List<ValidationQuestionEntity> questionEntities = response.getQuestions().stream()
                .map(question -> toQuestionEntity(saved, question))
                .toList();
        validationQuestionRepository.saveAll(questionEntities);

        return saved;
    }

    public ValidationReportEntity getReport(Long reportId) {
        return validationReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceValidationException("Relatorio nao encontrado"));
    }

    public List<ValidationIssueEntity> getIssues(Long reportId) {
        return validationIssueRepository.findByReportId(reportId);
    }

    public List<ValidationQuestionEntity> getQuestions(Long reportId) {
        return validationQuestionRepository.findByReportId(reportId);
    }

    private ValidationIssueEntity toIssueEntity(ValidationReportEntity report, AiValidationIssue issue) {
        return ValidationIssueEntity.builder()
                .report(report)
                .severity(issue.getSeverity())
                .category(issue.getCategory())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .suggestion(issue.getSuggestion())
                .build();
    }

    private ValidationQuestionEntity toQuestionEntity(ValidationReportEntity report, AiValidationQuestion question) {
        return ValidationQuestionEntity.builder()
                .report(report)
                .question(question.getQuestion())
                .reason(question.getReason())
                .targetAudience(question.getTargetAudience())
                .build();
    }

    public List<String> parseStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<SectionStatus> parseSectionStatusList(String json) {
        try {
            if (json == null || json.isBlank()) return List.of();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String toJsonObject(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            return "[]";
        }
    }
}
