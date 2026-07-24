package com.company.specvalidator.service.ai;

import com.company.specvalidator.dto.ai.AiValidationResponse;
import com.company.specvalidator.enums.ChecklistStatus;
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
                  "qualidade": "Alta",
                  "resumoExecutivo": "Documento bem elaborado, pronto para desenvolvimento",
                  "principaisRiscos": ["Volumetria nao informada"],
                  "specificationSummary": "EF descreve um relatorio ALV de ordens de producao",
                  "checklist": [
                    {
                      "chave": "regras_negocio",
                      "item": "Regras de negocio",
                      "status": "OK",
                      "comentario": "Regras descritas com SE/ENTAO claros"
                    },
                    {
                      "chave": "condicoes_teste",
                      "item": "Condicoes de teste",
                      "status": "Parcial",
                      "comentario": "Apenas o caminho feliz foi descrito"
                    }
                  ],
                  "pontosCriticos": [
                    {
                      "gap": "Cenarios de erro nao cobertos",
                      "impacto": "Risco de falha de teste em producao"
                    }
                  ],
                  "recomendacoes": ["Detalhar cenarios de teste de excecao"],
                  "parecerFinal": "Aprovado com ressalvas, ajustar cenarios de teste antes do QA"
                }
                """;
    }

    @Test
    void testParseValidJson() {
        AiValidationResponse response = parser.parse(validJson());

        assertNotNull(response);
        assertEquals("Alta", response.getQualidade());
        assertEquals("Documento bem elaborado, pronto para desenvolvimento", response.getResumoExecutivo());
        assertEquals(1, response.getPrincipaisRiscos().size());
        assertEquals("EF descreve um relatorio ALV de ordens de producao", response.getSpecificationSummary());
        assertEquals(2, response.getChecklist().size());
        assertEquals(ChecklistStatus.OK, response.getChecklist().get(0).getStatus());
        assertEquals(1, response.getPontosCriticos().size());
        assertEquals("Cenarios de erro nao cobertos", response.getPontosCriticos().get(0).getGap());
        assertEquals(1, response.getRecomendacoes().size());
        assertEquals("Aprovado com ressalvas, ajustar cenarios de teste antes do QA", response.getParecerFinal());
    }

    @Test
    void testParseChecklistStatusIsCaseInsensitive() {
        AiValidationResponse response = parser.parse(validJson());

        // "Parcial" (grafia usada no prompt) deve mapear para PARCIAL, nao cair no default AUSENTE
        assertEquals(ChecklistStatus.PARCIAL, response.getChecklist().get(1).getStatus());
    }

    @Test
    void testParseJsonWithMarkdownWrapper() {
        String wrapped = "```json\n" + validJson() + "\n```";
        AiValidationResponse response = parser.parse(wrapped);

        assertNotNull(response);
        assertEquals("Alta", response.getQualidade());
    }

    @Test
    void testParseJsonWithGenericMarkdownWrapper() {
        String wrapped = "```\n" + validJson() + "\n```";
        AiValidationResponse response = parser.parse(wrapped);

        assertNotNull(response);
        assertEquals("Alta", response.getQualidade());
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
    void testParseResponseWithReasoningBlockContainingStrayBraces() {
        // Simula o cenario real de producao: o texto livre do bloco RACIOCINIO
        // menciona uma estrutura com chaves, o que quebrava a extracao antiga
        // baseada em indexOf('{')/lastIndexOf('}').
        String response = """
                RACIOCINIO [
                Passo 9 (Estrutura de dados): documento cita a estrutura ZTABELA { CAMPO1, CAMPO2 } sem detalhar -> Parcial
                Conclusao: 1 gap relevante -> qualidade Media.
                ]

                """ + validJson();

        AiValidationResponse parsedResponse = parser.parse(response);

        assertNotNull(parsedResponse);
        assertEquals("Alta", parsedResponse.getQualidade());
    }

    @Test
    void testParseResponseWithoutReasoningBlockStillWorks() {
        // Quando a IA nao inclui o bloco RACIOCINIO (ex: mudanca futura de prompt),
        // o parser deve continuar funcionando normalmente.
        AiValidationResponse parsedResponse = parser.parse(validJson());

        assertNotNull(parsedResponse);
        assertEquals("Alta", parsedResponse.getQualidade());
    }

    @Test
    void testParseResponseWithLowQuality() {
        String json = """
                {
                  "qualidade": "Baixa",
                  "resumoExecutivo": "Documento insuficiente",
                  "principaisRiscos": ["Objetivo ausente", "Escopo ausente"],
                  "specificationSummary": "",
                  "checklist": [],
                  "pontosCriticos": [],
                  "recomendacoes": [],
                  "parecerFinal": "Reescrever a especificacao"
                }
                """;
        AiValidationResponse response = parser.parse(json);
        assertEquals("Baixa", response.getQualidade());
        assertEquals(2, response.getPrincipaisRiscos().size());
        assertEquals("Reescrever a especificacao", response.getParecerFinal());
    }
}
