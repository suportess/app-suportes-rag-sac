package com.company.specvalidator.service;

import com.company.specvalidator.dto.ai.AiValidationResponse;
import com.company.specvalidator.dto.ai.ChecklistItem;
import com.company.specvalidator.dto.ai.PontoCritico;
import com.company.specvalidator.entity.ChecklistItemEntity;
import com.company.specvalidator.entity.DocumentEntity;
import com.company.specvalidator.entity.PontoCriticoEntity;
import com.company.specvalidator.entity.ValidationReportEntity;
import com.company.specvalidator.exception.ResourceValidationException;
import com.company.specvalidator.repository.ChecklistItemRepository;
import com.company.specvalidator.repository.PontoCriticoRepository;
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
    private final ChecklistItemRepository checklistItemRepository;
    private final PontoCriticoRepository pontoCriticoRepository;
    private final ObjectMapper objectMapper;

    public ValidationReportService(ValidationReportRepository validationReportRepository,
                                   ChecklistItemRepository checklistItemRepository,
                                   PontoCriticoRepository pontoCriticoRepository,
                                   ObjectMapper objectMapper) {
        this.validationReportRepository = validationReportRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.pontoCriticoRepository = pontoCriticoRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ValidationReportEntity saveReport(DocumentEntity document, AiValidationResponse response) {
        ValidationReportEntity report = ValidationReportEntity.builder()
                .document(document)
                .classificacao(response.getClassificacao())
                .score(response.getScore())
                .specificationSummary(response.getSpecificationSummary())
                .qualidade(response.getQualidade())
                .resumoExecutivo(response.getResumoExecutivo())
                .principaisRiscosJson(toJson(response.getPrincipaisRiscos()))
                .recomendacoesJson(toJson(response.getRecomendacoes()))
                .parecerFinal(response.getParecerFinal())
                .sectionAnalysisJson(toJsonObject(response.getSectionAnalysis()))
                .build();

        ValidationReportEntity saved = validationReportRepository.save(report);

        List<ChecklistItemEntity> checklistEntities = response.getChecklist().stream()
                .map(item -> toChecklistItemEntity(saved, item))
                .toList();
        checklistItemRepository.saveAll(checklistEntities);

        List<PontoCriticoEntity> pontoCriticoEntities = response.getPontosCriticos().stream()
                .map(ponto -> toPontoCriticoEntity(saved, ponto))
                .toList();
        pontoCriticoRepository.saveAll(pontoCriticoEntities);

        return saved;
    }

    public ValidationReportEntity getReport(Long reportId) {
        return validationReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceValidationException("Relatorio nao encontrado"));
    }

    public List<ChecklistItemEntity> getChecklist(Long reportId) {
        return checklistItemRepository.findByReportId(reportId);
    }

    public List<PontoCriticoEntity> getPontosCriticos(Long reportId) {
        return pontoCriticoRepository.findByReportId(reportId);
    }

    private ChecklistItemEntity toChecklistItemEntity(ValidationReportEntity report, ChecklistItem item) {
        return ChecklistItemEntity.builder()
                .report(report)
                .chave(item.getChave())
                .item(item.getItem())
                .status(item.getStatus())
                .comentario(item.getComentario())
                .pontos(item.getPontos())
                .peso(item.getPeso())
                .pontosConquistados(item.getPontosConquistados())
                .build();
    }

    private PontoCriticoEntity toPontoCriticoEntity(ValidationReportEntity report, PontoCritico ponto) {
        return PontoCriticoEntity.builder()
                .report(report)
                .gap(ponto.getGap())
                .impacto(ponto.getImpacto())
                .build();
    }

    public List<String> parseStringList(String json) {
        try {
            if (json == null || json.isBlank()) return List.of();
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
