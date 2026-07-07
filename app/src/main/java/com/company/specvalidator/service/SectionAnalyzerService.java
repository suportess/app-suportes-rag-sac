package com.company.specvalidator.service;

import com.company.specvalidator.dto.response.SectionStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SectionAnalyzerService {

    private record MandatorySection(String canonicalName, List<String> keywords) {}

    private static final List<MandatorySection> MANDATORY_SECTIONS = List.of(
            new MandatorySection("Objetivo", List.of(
                    "objetivo", "introdução", "introducao", "finalidade", "propósito", "proposito", "visão geral", "visao geral")),
            new MandatorySection("Escopo", List.of(
                    "escopo", "abrangência", "abrangencia", "fora de escopo")),
            new MandatorySection("Regras de Negócio", List.of(
                    "regra", "regras de negocio", "regras de negócio", "regra de negocio", "regra de negócio")),
            new MandatorySection("Fluxo do Processo", List.of(
                    "fluxo", "processo", "diagrama", "sequência", "sequencia")),
            new MandatorySection("Objetos Técnicos", List.of(
                    "objeto", "técnico", "tecnico", "campos sap", "tabelas sap", "bapi", "badi", "estrutura")),
            new MandatorySection("Transações SAP", List.of(
                    "transação", "transacao", "transações", "transacoes")),
            new MandatorySection("Integrações", List.of(
                    "integração", "integracao", "interface", "interfaces")),
            new MandatorySection("Tratamento de Erros", List.of(
                    "erro", "erros", "log", "tratamento de erro", "tratamento de erros", "falha", "exception")),
            new MandatorySection("Cenários de Teste", List.of(
                    "teste", "testes", "cenário", "cenario", "caso de teste", "casos de teste")),
            new MandatorySection("Perfis e Autorizações", List.of(
                    "autorização", "autorizacao", "autorizações", "autorizacoes", "perfil", "perfis", "segurança", "seguranca")),
            new MandatorySection("Responsáveis", List.of(
                    "responsável", "responsavel", "responsáveis", "responsaveis", "contato")),
            new MandatorySection("Volume e Frequência", List.of(
                    "volume", "frequência", "frequencia", "carga", "quantidade"))
    );

    public List<SectionStatus> analyze(Map<String, String> sections, String rawText) {
        if (sections != null && !sections.isEmpty()) {
            return analyzeByStructure(sections);
        }
        return analyzeByText(rawText);
    }

    private List<SectionStatus> analyzeByStructure(Map<String, String> sections) {
        List<String> headings = List.copyOf(sections.keySet());
        return MANDATORY_SECTIONS.stream()
                .map(mandatory -> matchHeading(mandatory, sections, headings))
                .toList();
    }

    private SectionStatus matchHeading(MandatorySection mandatory, Map<String, String> sections, List<String> headings) {
        for (String heading : headings) {
            String headingLower = heading.toLowerCase();
            boolean matches = mandatory.keywords().stream()
                    .anyMatch(kw -> headingLower.contains(kw.toLowerCase()));
            if (matches) {
                String content = sections.getOrDefault(heading, "");
                String status = content.trim().length() < 80 ? "PARCIAL" : "PRESENTE";
                return SectionStatus.builder()
                        .sectionName(mandatory.canonicalName())
                        .status(status)
                        .detectedHeading(heading)
                        .build();
            }
        }
        return SectionStatus.builder()
                .sectionName(mandatory.canonicalName())
                .status("AUSENTE")
                .build();
    }

    private List<SectionStatus> analyzeByText(String rawText) {
        String textLower = rawText == null ? "" : rawText.toLowerCase();
        return MANDATORY_SECTIONS.stream()
                .map(mandatory -> {
                    boolean found = mandatory.keywords().stream()
                            .anyMatch(kw -> textLower.contains(kw.toLowerCase()));
                    return SectionStatus.builder()
                            .sectionName(mandatory.canonicalName())
                            .status(found ? "PRESENTE" : "AUSENTE")
                            .build();
                })
                .toList();
    }
}
