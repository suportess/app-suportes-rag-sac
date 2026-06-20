package com.company.specvalidator.service.ai;

import com.company.specvalidator.dto.ai.AiValidationResponse;
import com.company.specvalidator.enums.ValidationStatus;
import com.company.specvalidator.exception.AiProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiResponseParserTest {

    private AiResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new AiResponseParser(new ObjectMapper());
    }

    private String validJson() {
        return """
                {
                  "status": "APPROVED",
                  "score": 85,
                  "summary": "Documento bem elaborado",
                  "finalRecommendation": "Aprovado para desenvolvimento",
                  "issues": [
                    {
                      "severity": "MINOR",
                      "category": "ESTRUTURA",
                      "title": "Secao de escopo incompleta",
                      "description": "A secao de escopo poderia ser mais detalhada",
                      "suggestion": "Detalhar melhor o escopo"
                    }
                  ],
                  "questions": [
                    {
                      "question": "Qual a volumetria esperada?",
                      "reason": "Para dimensionar performance",
                      "targetAudience": "FUNCIONAL"
                    }
                  ],
                  "positivePoints": [
                    "Objetivo claro",
                    "Tabelas SAP bem referenciadas"
                  ],
                  "missingSections": [
                    "Perfis e autorizacoes"
                  ],
                  "riskAnalysis": "Risco baixo para implementacao"
                }
                """;
    }

    @Test
    void testParseValidJson() {
        AiValidationResponse response = parser.parse(validJson());

        assertNotNull(response);
        assertEquals(ValidationStatus.APPROVED, response.getStatus());
        assertEquals(85, response.getScore());
        assertEquals("Documento bem elaborado", response.getSummary());
        assertEquals("Aprovado para desenvolvimento", response.getFinalRecommendation());
        assertEquals(1, response.getIssues().size());
        assertEquals("Secao de escopo incompleta", response.getIssues().get(0).getTitle());
        assertEquals(1, response.getQuestions().size());
        assertEquals(2, response.getPositivePoints().size());
        assertEquals(1, response.getMissingSections().size());
        assertEquals("Risco baixo para implementacao", response.getRiskAnalysis());
    }

    @Test
    void testParseJsonWithMarkdownWrapper() {
        String wrapped = "```json\n" + validJson() + "\n```";
        AiValidationResponse response = parser.parse(wrapped);

        assertNotNull(response);
        assertEquals(ValidationStatus.APPROVED, response.getStatus());
        assertEquals(85, response.getScore());
    }

    @Test
    void testParseJsonWithGenericMarkdownWrapper() {
        String wrapped = "```\n" + validJson() + "\n```";
        AiValidationResponse response = parser.parse(wrapped);

        assertNotNull(response);
        assertEquals(ValidationStatus.APPROVED, response.getStatus());
    }

    @Test
    void testParseInvalidJsonThrowsException() {
        String invalidJson = "{ this is not valid json }";
        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> parser.parse(invalidJson));
        assertTrue(exception.getMessage().contains("JSON invalido"),
                "Exception message should mention invalid JSON");
    }

    @Test
    void testParseNullThrowsException() {
        // cleanup converts null to "" which is still invalid JSON
        assertThrows(AiProviderException.class, () -> parser.parse(null));
    }

    @Test
    void testParseEmptyStringThrowsException() {
        assertThrows(AiProviderException.class, () -> parser.parse(""));
    }

    @Test
    void testParseResponseWithRejectedStatus() {
        String json = """
                {
                  "status": "REJECTED",
                  "score": 30,
                  "summary": "Documento insuficiente",
                  "finalRecommendation": "Reescrever a especificacao",
                  "issues": [],
                  "questions": [],
                  "positivePoints": [],
                  "missingSections": ["Objetivo", "Escopo"],
                  "riskAnalysis": "Risco alto"
                }
                """;
        AiValidationResponse response = parser.parse(json);
        assertEquals(ValidationStatus.REJECTED, response.getStatus());
        assertEquals(30, response.getScore());
        assertEquals(2, response.getMissingSections().size());
    }
}
