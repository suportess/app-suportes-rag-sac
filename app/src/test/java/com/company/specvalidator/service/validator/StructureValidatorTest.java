package com.company.specvalidator.service.validator;

import com.company.specvalidator.dto.ai.AiValidationIssue;
import com.company.specvalidator.enums.IssueSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StructureValidatorTest {

    private StructureValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StructureValidator();
    }

    @Test
    void testCompleteDocumentReturnsNoIssues() {
        String text = "Este documento descreve o objetivo da solucao.\n" +
                "Criterios de aceite definidos pelo funcional.\n" +
                "Cenarios de teste incluidos.\n" +
                "Tabela MARA e transacao MM01 utilizadas.\n" +
                "BAPI_PO_CREATE chamada na integracao.";

        List<AiValidationIssue> issues = validator.validate(text);
        assertTrue(issues.isEmpty(),
                "A complete document should produce no validation issues");
    }

    @Test
    void testMissingObjetivoReturnsModerateIssue() {
        String text = "Criterios de aceite definidos.\n" +
                "Cenarios de teste incluidos.\n" +
                "Tabela MARA referenciada.";

        List<AiValidationIssue> issues = validator.validate(text);
        assertTrue(issues.stream()
                        .anyMatch(i -> i.getSeverity() == IssueSeverity.MODERATE
                                && i.getTitle().contains("objetivo")),
                "Missing 'objetivo' should produce a MODERATE issue");
    }

    @Test
    void testMissingCriteriosDeAceiteReturnsCriticalIssue() {
        String text = "Objetivo claro da solucao.\n" +
                "Cenarios de teste incluidos.\n" +
                "Tabela MARA referenciada.";

        List<AiValidationIssue> issues = validator.validate(text);
        assertTrue(issues.stream()
                        .anyMatch(i -> i.getSeverity() == IssueSeverity.CRITICAL
                                && i.getCategory().equals("TESTES")
                                && i.getTitle().contains("aceite")),
                "Missing 'criterios de aceite' should produce a CRITICAL issue");
    }

    @Test
    void testMissingTestesReturnsModerateIssue() {
        // Note: "criterio de aceite" satisfies the acceptance criteria check,
        // but we must avoid the word "teste" entirely to trigger the test-scenarios check
        String text = "Objetivo claro da solucao.\n" +
                "Criterio de aceite definido.\n" +
                "Tabela MARA referenciada.";

        List<AiValidationIssue> issues = validator.validate(text);
        assertTrue(issues.stream()
                        .anyMatch(i -> i.getSeverity() == IssueSeverity.MODERATE
                                && i.getTitle().toLowerCase().contains("teste")),
                "Missing test scenarios should produce a MODERATE issue");
    }

    @Test
    void testMissingSapTermsReturnsCriticalIssue() {
        String text = "Objetivo claro da solucao.\n" +
                "Criterios de aceite definidos.\n" +
                "Cenarios de teste incluidos.";

        List<AiValidationIssue> issues = validator.validate(text);
        assertTrue(issues.stream()
                        .anyMatch(i -> i.getSeverity() == IssueSeverity.CRITICAL
                                && i.getCategory().equals("SAP_ABAP")),
                "Missing SAP terms should produce a CRITICAL issue");
    }

    @Test
    void testEmptyTextReturnsAllIssues() {
        List<AiValidationIssue> issues = validator.validate("");
        assertEquals(4, issues.size(),
                "Empty text should trigger all 4 validation checks");
    }

    @Test
    void testNullTextReturnsAllIssues() {
        List<AiValidationIssue> issues = validator.validate(null);
        assertEquals(4, issues.size(),
                "Null text should trigger all 4 validation checks");
    }
}
