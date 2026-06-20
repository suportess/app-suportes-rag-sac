package com.company.specvalidator.controller;

import com.company.specvalidator.dto.response.ValidationReportResponse;
import com.company.specvalidator.entity.ValidationReportEntity;
import com.company.specvalidator.mapper.ValidationReportMapper;
import com.company.specvalidator.service.ValidationReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/validations")
@Tag(name = "Validations", description = "Consulta de relatorios de validacao")
public class ValidationController {

    private final ValidationReportService validationReportService;
    private final ValidationReportMapper validationReportMapper;

    public ValidationController(ValidationReportService validationReportService,
                                ValidationReportMapper validationReportMapper) {
        this.validationReportService = validationReportService;
        this.validationReportMapper = validationReportMapper;
    }

    @Operation(summary = "Buscar relatorio de validacao por ID")
    @GetMapping("/{reportId}")
    public ValidationReportResponse getReport(@PathVariable Long reportId) {
        ValidationReportEntity report = validationReportService.getReport(reportId);
        return validationReportMapper.toResponse(
                report,
                validationReportService.getIssues(reportId),
                validationReportService.getQuestions(reportId),
                validationReportService.parseStringList(report.getPositivePointsJson()),
                validationReportService.parseStringList(report.getMissingSectionsJson()),
                report.getRiskAnalysis()
        );
    }
}
