package com.company.specvalidator.service.validator;

import com.company.specvalidator.dto.ai.AiValidationIssue;
import com.company.specvalidator.enums.IssueSeverity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class TestScenarioValidator {

    public List<AiValidationIssue> validate(String normalizedText) {
        String text = normalizedText == null ? "" : normalizedText.toLowerCase(Locale.ROOT);
        List<AiValidationIssue> issues = new ArrayList<>();

        // Check if actual test scenarios are present (not just section titles)
        boolean hasCenario = containsAny(text, "cenario", "teste");
        boolean hasScenarioContent = containsAny(text, "cenario 1", "cenario 01", "ct01", "ct02",
                "caso de teste", "passo 1", "passo 01", "step", "dado que", "entao", "quando o usuario",
                "pre-condicao", "pre condicao", "pos-condicao", "pos condicao");
        if (hasCenario && !hasScenarioContent) {
            issues.add(issue("TESTES", "Cenarios de teste superficiais",
                    "Documento menciona testes mas nao detalha cenarios com passos, pre-condicoes e pos-condicoes.",
                    "Descrever cada cenario de teste com: pre-condicao, passos de execucao, dados de entrada e resultado esperado.",
                    IssueSeverity.MODERATE));
        }

        // Check for both positive AND negative test cases
        boolean hasPositive = containsAny(text, "sucesso", "caminho feliz", "fluxo principal",
                "cenario positivo", "caso positivo", "resultado correto");
        boolean hasNegative = containsAny(text, "erro", "falha", "excecao", "invalido", "negativo",
                "caminho alternativo", "cenario negativo", "caso negativo", "rejeicao", "rejeitar");
        if (hasCenario) {
            if (!hasPositive && !hasNegative) {
                issues.add(issue("TESTES", "Cenarios positivos e negativos ausentes",
                        "Documento nao descreve cenarios de teste positivos (sucesso) nem negativos (erro/excecao).",
                        "Incluir cenarios de teste para fluxo principal (sucesso) e fluxos alternativos (erro, excecao, dados invalidos).",
                        IssueSeverity.CRITICAL));
            } else if (!hasNegative) {
                issues.add(issue("TESTES", "Cenarios negativos ausentes",
                        "Documento descreve apenas cenarios de sucesso, sem cobrir erros ou excecoes.",
                        "Adicionar cenarios de teste negativos: dados invalidos, erros de processamento, limites excedidos.",
                        IssueSeverity.MODERATE));
            } else if (!hasPositive) {
                issues.add(issue("TESTES", "Cenarios positivos ausentes",
                        "Documento descreve cenarios de erro mas nao detalha o fluxo principal de sucesso.",
                        "Adicionar cenarios de teste positivos que validem o fluxo principal com dados validos.",
                        IssueSeverity.MODERATE));
            }
        }

        // Check for test data requirements
        if (!containsAny(text, "massa de teste", "dados de teste", "dados de entrada", "amostra",
                "exemplo de dados", "base de teste", "carga de teste")) {
            issues.add(issue("TESTES", "Massa de teste nao especificada",
                    "Documento nao descreve requisitos de massa ou dados de teste.",
                    "Especificar a massa de teste necessaria: dados de entrada, volume esperado e como obter/gerar os dados.",
                    IssueSeverity.MINOR));
        }

        // Check for expected results documentation
        if (!containsAny(text, "resultado esperado", "saida esperada", "retorno esperado",
                "comportamento esperado", "deve retornar", "deve exibir", "deve gerar",
                "espera-se", "esperado")) {
            issues.add(issue("TESTES", "Resultados esperados nao documentados",
                    "Documento nao define claramente os resultados esperados para os testes.",
                    "Para cada cenario de teste, documentar o resultado esperado: saidas, mensagens e estados finais.",
                    IssueSeverity.CRITICAL));
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
