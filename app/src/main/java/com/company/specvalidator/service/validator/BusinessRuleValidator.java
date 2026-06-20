package com.company.specvalidator.service.validator;

import com.company.specvalidator.dto.ai.AiValidationIssue;
import com.company.specvalidator.enums.IssueSeverity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class BusinessRuleValidator {

    public List<AiValidationIssue> validate(String normalizedText) {
        String text = normalizedText == null ? "" : normalizedText.toLowerCase(Locale.ROOT);
        List<AiValidationIssue> issues = new ArrayList<>();

        // Check if decision conditions are described
        if (!containsAny(text, " se ", "quando", "caso ", "condicao", "condicoes", "regra de negocio",
                "regra", "criterio", "logica")) {
            issues.add(issue("REGRA_NEGOCIO", "Condicoes de decisao ausentes",
                    "Documento nao descreve condicoes de decisao ou regras de negocio claras.",
                    "Detalhar as condicoes de decisao utilizando estrutura SE/ENTAO/SENAO para cada regra.",
                    IssueSeverity.CRITICAL));
        }

        // Check for exception/alternative flow handling in business rules
        if (containsAny(text, "regra", "condicao", " se ", "quando")) {
            if (!containsAny(text, "senao", "caso contrario", "excecao", "alternativ", "fluxo alternativo",
                    "tratamento", "fallback", "default")) {
                issues.add(issue("REGRA_NEGOCIO", "Fluxos alternativos nao descritos",
                        "Regras de negocio foram identificadas mas nao ha descricao de fluxos alternativos ou excecoes.",
                        "Para cada regra, descrever o comportamento quando a condicao nao e atendida (SENAO/CASO CONTRARIO).",
                        IssueSeverity.MODERATE));
            }
        }

        // Check for numeric thresholds when calculations are involved
        if (containsAny(text, "calculo", "calcular", "valor", "percentual", "porcentagem", "soma",
                "total", "media", "formula")) {
            if (!containsAny(text, "limite", "maximo", "minimo", "threshold", "faixa", "intervalo",
                    "maior que", "menor que", "entre", "acima", "abaixo")) {
                issues.add(issue("REGRA_NEGOCIO", "Limites e faixas nao definidos",
                        "Documento menciona calculos mas nao define limites, faixas ou valores de referencia.",
                        "Especificar valores limites, faixas aceitas e comportamento em valores extremos.",
                        IssueSeverity.MODERATE));
            }
        }

        // Check if responsible actors are defined
        if (!containsAny(text, "usuario", "aprovador", "gestor", "responsavel", "papel", "role",
                "perfil", "solicitante", "executor")) {
            issues.add(issue("REGRA_NEGOCIO", "Atores responsaveis nao definidos",
                    "Documento nao identifica os atores ou papeis responsaveis pelas acoes.",
                    "Definir claramente os atores envolvidos: solicitante, aprovador, executor e seus papeis.",
                    IssueSeverity.MODERATE));
        }

        // Check for workflow/approval steps when approval is mentioned
        if (containsAny(text, "aprovacao", "aprovar", "aprovador", "workflow", "fluxo de aprovacao")) {
            if (!containsAny(text, "etapa", "passo", "nivel", "hierarquia", "sequencia",
                    "primeiro", "segundo", "depois", "apos", "ordem de aprovacao")) {
                issues.add(issue("REGRA_NEGOCIO", "Etapas de aprovacao nao detalhadas",
                        "Documento menciona aprovacao mas nao detalha as etapas ou niveis do fluxo.",
                        "Descrever as etapas do workflow de aprovacao: niveis, hierarquia, prazos e acoes em cada etapa.",
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
