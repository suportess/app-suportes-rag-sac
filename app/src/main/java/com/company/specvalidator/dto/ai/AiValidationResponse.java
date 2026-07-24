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
    private String qualidade;
    private String resumoExecutivo;
    @Builder.Default
    private List<String> principaisRiscos = new ArrayList<>();
    private String specificationSummary;
    @Builder.Default
    private List<ChecklistItem> checklist = new ArrayList<>();
    @Builder.Default
    private List<PontoCritico> pontosCriticos = new ArrayList<>();
    @Builder.Default
    private List<String> recomendacoes = new ArrayList<>();
    private String parecerFinal;
    private Integer score;
    private ValidationStatus classificacao;
    @Builder.Default
    private List<SectionStatus> sectionAnalysis = new ArrayList<>();
}
