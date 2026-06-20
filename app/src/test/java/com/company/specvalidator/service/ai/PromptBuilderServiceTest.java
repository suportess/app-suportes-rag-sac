package com.company.specvalidator.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderServiceTest {

    private PromptBuilderService service;

    @BeforeEach
    void setUp() {
        service = new PromptBuilderService();
    }

    @Test
    void testPromptContainsDocumentText() {
        String documentText = "Minha especificacao funcional SAP ABAP para modulo MM.";
        String prompt = service.buildValidationPrompt(documentText);
        assertTrue(prompt.contains(documentText),
                "The generated prompt should contain the original document text");
    }

    @Test
    void testPromptContainsJsonFormat() {
        String prompt = service.buildValidationPrompt("qualquer texto");
        assertTrue(prompt.contains("JSON"),
                "The prompt should instruct the AI to return JSON format");
        assertTrue(prompt.contains("\"status\""),
                "The prompt should include the expected JSON field 'status'");
        assertTrue(prompt.contains("\"score\""),
                "The prompt should include the expected JSON field 'score'");
        assertTrue(prompt.contains("\"issues\""),
                "The prompt should include the expected JSON field 'issues'");
    }

    @Test
    void testPromptContainsSeverityLevels() {
        String prompt = service.buildValidationPrompt("qualquer texto");
        assertTrue(prompt.contains("CRITICAL"),
                "The prompt should mention CRITICAL severity");
        assertTrue(prompt.contains("MODERATE"),
                "The prompt should mention MODERATE severity");
        assertTrue(prompt.contains("MINOR"),
                "The prompt should mention MINOR severity");
    }

    @Test
    void testPromptContainsEvaluationCriteria() {
        String prompt = service.buildValidationPrompt("qualquer texto");
        assertTrue(prompt.contains("objetivo"),
                "The prompt should mention 'objetivo' as evaluation criteria");
        assertTrue(prompt.contains("Regras de negocio"),
                "The prompt should mention 'Regras de negocio' as evaluation criteria");
        assertTrue(prompt.contains("Cenarios de teste"),
                "The prompt should mention 'Cenarios de teste' as evaluation criteria");
        assertTrue(prompt.contains("Criterios de aceite"),
                "The prompt should mention 'Criterios de aceite' as evaluation criteria");
        assertTrue(prompt.contains("Tabelas SAP"),
                "The prompt should mention 'Tabelas SAP' as evaluation criteria");
    }

    @Test
    void testDocumentTextAppearsAtEnd() {
        String documentText = "UNIQUE_DOCUMENT_MARKER_12345";
        String prompt = service.buildValidationPrompt(documentText);
        String afterDocText = prompt.substring(prompt.indexOf(documentText) + documentText.length()).trim();
        assertTrue(afterDocText.isEmpty(),
                "The document text should appear at the end of the prompt");
    }
}
