package com.company.specvalidator.service.validator;

import com.company.specvalidator.dto.ai.AiValidationIssue;
import com.company.specvalidator.enums.IssueSeverity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class StructureValidator {

    public List<AiValidationIssue> validate(String normalizedText) {
        String text = normalizedText == null ? "" : normalizedText.toLowerCase(Locale.ROOT);
        List<AiValidationIssue> issues = new ArrayList<>();

        if (!containsAny(text, "objetivo")) {
            issues.add(issue("ESTRUTURA", "Secao de objetivo ausente",
                    "Documento nao apresenta claramente o objetivo da solucao.",
                    "Incluir secao Objetivo com contexto de negocio e resultado esperado.",
                    IssueSeverity.MODERATE));
        }

        if (!containsAny(text, "criterio de aceite", "criterios de aceite")) {
            issues.add(issue("TESTES", "Criterios de aceite ausentes",
                    "Sem criterios de aceite o time ABAP pode implementar sem validacao objetiva.",
                    "Definir criterios de aceite mensuraveis e aprovados pelo funcional.",
                    IssueSeverity.CRITICAL));
        }

        if (!containsAny(text, "teste", "cenario de teste", "cenarios de teste")) {
            issues.add(issue("TESTES", "Cenarios de teste ausentes",
                    "Documento nao descreve cenarios de teste funcionais e de excecao.",
                    "Adicionar cenarios de teste com entradas, processamento e resultado esperado.",
                    IssueSeverity.MODERATE));
        }

        if (!containsAny(text, "tabela", "transacao", "bapi", "badi", "user exit", "enhancement", "report")) {
            issues.add(issue("SAP_ABAP", "Referencias SAP ABAP insuficientes",
                    "Nao foram encontrados termos SAP chave para orientar implementacao tecnica.",
                    "Indicar tabelas, transacoes, objetos de extensao e pontos de integracao.",
                    IssueSeverity.CRITICAL));
        }

        return issues;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private AiValidationIssue issue(String category, String title, String description, String suggestion, IssueSeverity severity) {
        return AiValidationIssue.builder()
                .category(category)
                .title(title)
                .description(description)
                .suggestion(suggestion)
                .severity(severity)
                .build();
    }
}
