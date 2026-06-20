package com.company.specvalidator.service.validator;

import com.company.specvalidator.dto.ai.AiValidationIssue;
import com.company.specvalidator.enums.IssueSeverity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class IntegrationValidator {

    public List<AiValidationIssue> validate(String normalizedText) {
        String text = normalizedText == null ? "" : normalizedText.toLowerCase(Locale.ROOT);
        List<AiValidationIssue> issues = new ArrayList<>();

        boolean mentionsIntegration = containsAny(text, "integracao", "integrar", "interface",
                "sistema externo", "servico externo", "api");

        // Check for integration technology specification
        if (mentionsIntegration) {
            if (!containsAny(text, "rfc", "bapi", "idoc", "pi/po", "cpi", "sap pi", "sap po",
                    "sap cpi", "web service", "rest", "soap", "odata", "api gateway")) {
                issues.add(issue("INTEGRACAO", "Tecnologia de integracao nao especificada",
                        "Documento menciona integracao mas nao especifica a tecnologia (RFC, BAPI, IDoc, PI/PO, CPI, REST).",
                        "Definir a tecnologia de integracao a ser utilizada e justificar a escolha.",
                        IssueSeverity.CRITICAL));
            }
        }

        // Check for interface direction
        if (mentionsIntegration) {
            if (!containsAny(text, "entrada", "saida", "inbound", "outbound", "envio", "recebimento",
                    "receber", "enviar", "origem", "destino", "request", "response")) {
                issues.add(issue("INTEGRACAO", "Direcao da interface nao especificada",
                        "Documento nao indica a direcao do fluxo de dados na integracao (entrada/saida).",
                        "Especificar a direcao da interface: sistema de origem, destino e sentido do fluxo de dados.",
                        IssueSeverity.MODERATE));
            }
        }

        // Check for error handling in integration
        if (mentionsIntegration) {
            if (!containsAny(text, "erro de integracao", "falha de comunicacao", "timeout",
                    "retry", "reprocessamento", "contingencia", "fallback", "fila de erro",
                    "log de erro", "monitoramento", "alerta", "notificacao de erro")) {
                issues.add(issue("INTEGRACAO", "Tratamento de falhas de integracao ausente",
                        "Documento nao descreve como tratar falhas na integracao (timeout, indisponibilidade, erros).",
                        "Definir estrategia de tratamento de erros: retentativas, contingencia, alertas e reprocessamento.",
                        IssueSeverity.CRITICAL));
            }
        }

        // Check for data format/mapping documentation
        if (mentionsIntegration) {
            if (!containsAny(text, "mapeamento", "mapping", "layout", "formato", "campo",
                    "de-para", "estrutura", "json", "xml", "csv", "flat file", "arquivo")) {
                issues.add(issue("INTEGRACAO", "Mapeamento de dados nao documentado",
                        "Documento menciona integracao mas nao documenta o mapeamento ou formato dos dados.",
                        "Incluir o mapeamento de campos (de-para), formato dos dados e estrutura das mensagens.",
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
