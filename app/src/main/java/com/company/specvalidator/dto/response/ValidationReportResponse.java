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
    private String qualidade;
    private String resumoExecutivo;
    private List<String> principaisRiscos;
    private String specificationSummary;
    private List<ChecklistItemResponse> checklist;
    private List<PontoCriticoResponse> pontosCriticos;
    private List<String> recomendacoes;
    private String parecerFinal;
    private Integer score;
    private ValidationStatus classificacao;
    private List<SectionStatus> sectionAnalysis;
}
