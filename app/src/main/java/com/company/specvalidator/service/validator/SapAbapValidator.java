package com.company.specvalidator.service.validator;

import com.company.specvalidator.dto.ai.AiValidationIssue;
import com.company.specvalidator.enums.IssueSeverity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SapAbapValidator {

    private static final Pattern SAP_TABLE_PATTERN = Pattern.compile("\\b[zy][a-z0-9_]{2,}\\b");

    public List<AiValidationIssue> validate(String normalizedText) {
        String text = normalizedText == null ? "" : normalizedText.toLowerCase(Locale.ROOT);
        List<AiValidationIssue> issues = new ArrayList<>();

        // Check for specific SAP table references (Z*, Y* custom tables)
        if (!SAP_TABLE_PATTERN.matcher(text).find()
                && !containsAny(text, "mara", "marc", "mard", "vbak", "vbap", "ekko", "ekpo", "bkpf", "bseg", "likp", "lips")) {
            issues.add(issue("SAP_ABAP", "Tabelas SAP nao especificadas",
                    "Documento nao menciona tabelas SAP especificas (customizadas Z*/Y* ou standard).",
                    "Indicar as tabelas SAP envolvidas na solucao, incluindo tabelas de leitura e gravacao.",
                    IssueSeverity.MODERATE));
        }

        // Check for authorization object concerns
        if (!containsAny(text, "su53", "objeto de autorizacao", "auth object", "perfil de autorizacao",
                "autorizacao", "autoriza", "perfil de acesso", "controle de acesso")) {
            issues.add(issue("SAP_ABAP", "Autorizacoes nao mencionadas",
                    "Documento nao aborda verificacao de autorizacoes SAP.",
                    "Especificar objetos de autorizacao necessarios e perfis de acesso envolvidos.",
                    IssueSeverity.MODERATE));
        }

        // Check for transport/request concerns
        if (!containsAny(text, "request", "transport", "ordem de transporte", "ordem", "landscape",
                "dev", "qas", "prd", "ambiente")) {
            issues.add(issue("SAP_ABAP", "Estrategia de transporte nao definida",
                    "Documento nao menciona ordens de transporte ou estrategia de deploy entre ambientes.",
                    "Descrever a estrategia de transporte e os ambientes envolvidos (DEV, QAS, PRD).",
                    IssueSeverity.MINOR));
        }

        // Check for error/message handling
        if (!containsAny(text, "mensagem de erro", "mensagem", "sy-subrc", "exception", "excecao",
                "tratamento de erro", "classe de mensagem", "message class", "raise")) {
            issues.add(issue("SAP_ABAP", "Tratamento de erros e mensagens ausente",
                    "Documento nao descreve tratamento de erros ou mensagens para o usuario.",
                    "Definir mensagens de erro, aviso e sucesso, incluindo classe de mensagem e textos.",
                    IssueSeverity.CRITICAL));
        }

        // Check for performance considerations in batch/mass processing
        if (containsAny(text, "massa", "batch", "job", "lote", "processamento em massa", "carga",
                "volume", "milhares", "milhoes")) {
            if (!containsAny(text, "performance", "desempenho", "pacote", "commit", "package size",
                    "paralelismo", "indice", "index", "otimiza")) {
                issues.add(issue("SAP_ABAP", "Consideracoes de performance ausentes",
                        "Documento menciona processamento em massa/batch mas nao aborda performance.",
                        "Incluir requisitos de performance: tamanho de pacote, commits intermediarios, indices e paralelismo.",
                        IssueSeverity.CRITICAL));
            }
        }

        return issues;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }

    private AiValidationIssue issue(String category, String title, String description, String suggestion, IssueSeverity severity) {
        return AiValidationIssue.builder()
                .category(category).title(title).description(description).suggestion(suggestion).severity(severity)
                .build();
    }
}
