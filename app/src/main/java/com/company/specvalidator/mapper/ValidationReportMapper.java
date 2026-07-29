package com.company.specvalidator.mapper;

import com.company.specvalidator.dto.response.ChecklistItemResponse;
import com.company.specvalidator.dto.response.PontoCriticoResponse;
import com.company.specvalidator.dto.response.SectionStatus;
import com.company.specvalidator.dto.response.ValidationReportResponse;
import com.company.specvalidator.entity.ChecklistItemEntity;
import com.company.specvalidator.entity.PontoCriticoEntity;
import com.company.specvalidator.entity.ValidationReportEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidationReportMapper {

    public ValidationReportResponse toResponse(
            ValidationReportEntity report,
            List<ChecklistItemEntity> checklist,
            List<PontoCriticoEntity> pontosCriticos,
            List<String> principaisRiscos,
            List<String> recomendacoes,
            List<SectionStatus> sectionAnalysis) {

        return ValidationReportResponse.builder()
                .reportId(report.getId())
                .documentId(report.getDocument().getId())
                .qualidade(report.getQualidade())
                .resumoExecutivo(report.getResumoExecutivo())
                .principaisRiscos(principaisRiscos)
                .specificationSummary(report.getSpecificationSummary())
                .checklist(checklist.stream().map(this::toChecklistItemResponse).toList())
                .pontosCriticos(pontosCriticos.stream().map(this::toPontoCriticoResponse).toList())
                .recomendacoes(recomendacoes)
                .parecerFinal(report.getParecerFinal())
                .score(report.getScore())
                .classificacao(report.getClassificacao())
                .sectionAnalysis(sectionAnalysis)
                .build();
    }

    private ChecklistItemResponse toChecklistItemResponse(ChecklistItemEntity entity) {
        return ChecklistItemResponse.builder()
                .chave(entity.getChave())
                .item(entity.getItem())
                .status(entity.getStatus())
                .comentario(entity.getComentario())
                .pontos(entity.getPontos())
                .peso(entity.getPeso())
                .pontosConquistados(entity.getPontosConquistados())
                .aplicavel(entity.isAplicavel())
                .build();
    }

    private PontoCriticoResponse toPontoCriticoResponse(PontoCriticoEntity entity) {
        return PontoCriticoResponse.builder()
                .gap(entity.getGap())
                .impacto(entity.getImpacto())
                .build();
    }
}
