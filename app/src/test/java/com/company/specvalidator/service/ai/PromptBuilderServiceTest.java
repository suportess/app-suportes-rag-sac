package com.company.specvalidator.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.company.specvalidator.service.ai.PromptBuilderService.DevType.*;
import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderServiceTest {

    private PromptBuilderService service;

    @BeforeEach
    void setUp() {
        service = new PromptBuilderService();
    }

    // --- prompt structure ---

    @Test
    void testPromptContainsDocumentText() {
        String documentText = "Minha especificacao funcional SAP ABAP para modulo MM.";
        String prompt = service.buildValidationPrompt(documentText);
        assertTrue(prompt.contains(documentText));
    }

    @Test
    void testPromptContainsJsonFormat() {
        String prompt = service.buildValidationPrompt("qualquer texto");
        assertTrue(prompt.contains("JSON"));
        assertTrue(prompt.contains("\"status\""));
        assertTrue(prompt.contains("\"score\""));
        assertTrue(prompt.contains("\"issues\""));
    }

    @Test
    void testPromptContainsSeverityLevels() {
        String prompt = service.buildValidationPrompt("qualquer texto");
        assertTrue(prompt.contains("CRITICAL"));
        assertTrue(prompt.contains("MODERATE"));
        assertTrue(prompt.contains("MINOR"));
    }

    @Test
    void testPromptContainsGeneralCriteria() {
        String prompt = service.buildValidationPrompt("qualquer texto");
        assertTrue(prompt.contains("objetivo"));
        assertTrue(prompt.contains("Regras de negocio"));
        assertTrue(prompt.contains("Cenarios de teste"));
        assertTrue(prompt.contains("Tabelas SAP"));
        assertTrue(prompt.contains("Criterios de aceite"));
    }

    @Test
    void testDocumentTextAppearsAtEnd() {
        String documentText = "UNIQUE_DOCUMENT_MARKER_12345";
        String prompt = service.buildValidationPrompt(documentText);
        String afterDocText = prompt.substring(prompt.indexOf(documentText) + documentText.length()).trim();
        assertTrue(afterDocText.isEmpty());
    }

    // --- new critical rules ---

    @Test
    void testPromptEnforcesTriggerAsModerate() {
        String prompt = service.buildValidationPrompt("qualquer texto");
        assertTrue(prompt.contains("TRIGGER"));
        // transacao nomeada (QM01, VA01) = trigger presente, nao cobrar
        assertTrue(prompt.contains("trigger PRESENTE, NAO classificar como problema"));
    }

    @Test
    void testPromptEnforcesBadiWithoutNameAsCritical() {
        String prompt = service.buildValidationPrompt("qualquer texto");
        // BAPI/BADI sem nome tecnico exato = CRITICAL
        assertTrue(prompt.contains("SEM nome tecnico exato"));
        assertTrue(prompt.contains("CRITICAL, categoria SAP_ABAP"));
    }

    @Test
    void testIntegrationWithoutTechnologyIsModerate() {
        String prompt = service.buildValidationPrompt("qualquer texto");
        // integracao sem tecnologia = MODERATE, nao CRITICAL
        assertTrue(prompt.contains("mencionada sem tecnologia de comunicacao"));
        assertTrue(prompt.contains("= MODERATE"));
    }

    @Test
    void testPromptRequiresSecurityJustificationWhenNA() {
        String prompt = service.buildValidationPrompt("qualquer texto");
        assertTrue(prompt.contains("N/A"));
        assertTrue(prompt.contains("ARQUITETURA"));
    }

    @Test
    void testCriteriosDeAceiteAreNotEvaluated() {
        String prompt = service.buildValidationPrompt("qualquer texto");
        assertTrue(prompt.contains("NAO cobrar"));
    }

    // --- development type detection ---

    @Test
    void testDetectsReportType() {
        assertEquals(REPORT, service.detectDevType("Este relatorio ALV exibe os dados de saida"));
        assertEquals(REPORT, service.detectDevType("tela de selecao com variante de selecao padrao"));
    }

    @Test
    void testDetectsEnhancementType() {
        assertEquals(ENHANCEMENT, service.detectDevType("Implementar BADI MB_DOCUMENT_BADI para validar entrada"));
        assertEquals(ENHANCEMENT, service.detectDevType("usar enhancement point para interceptar"));
    }

    @Test
    void testDetectsInterfaceType() {
        assertEquals(INTERFACE, service.detectDevType("integracao via RFC com sistema externo"));
        assertEquals(INTERFACE, service.detectDevType("envio de IDoc inbound para recebimento de pedidos"));
    }

    @Test
    void testDetectsWorkflowType() {
        assertEquals(WORKFLOW, service.detectDevType("fluxo de aprovacao com workflow SAP"));
        assertEquals(WORKFLOW, service.detectDevType("aprovacao em dois niveis com aprovador hierarquico"));
    }

    @Test
    void testDetectsFormsType() {
        assertEquals(FORMS, service.detectDevType("formulario SmartForm para impressao de etiqueta"));
        assertEquals(FORMS, service.detectDevType("layout de impressao usando SapScript"));
    }

    @Test
    void testDetectsBatchType() {
        assertEquals(BATCH, service.detectDevType("carga via batch input com call transaction MIGO"));
        assertEquals(BATCH, service.detectDevType("sessao BDC para migracao de dados"));
    }

    @Test
    void testDetectsTableType() {
        assertEquals(TABLE, service.detectDevType("criar tabela customizada no SE11 com classe de entrega A"));
        assertEquals(TABLE, service.detectDevType("nova tabela z com dicionario abap e chave primaria definida"));
    }

    @Test
    void testReturnsUnknownWhenNoSignificantKeywords() {
        assertEquals(UNKNOWN, service.detectDevType("documento generico sem palavras chave especificas"));
        assertEquals(UNKNOWN, service.detectDevType(""));
        assertEquals(UNKNOWN, service.detectDevType(null));
    }

    // --- type-specific criteria in prompt ---

    @Test
    void testReportPromptContainsAlvCriteria() {
        String prompt = service.buildValidationPrompt("relatorio ALV para exibir dados de material");
        assertTrue(prompt.contains("REPORT/RELATORIO ALV"));
        assertTrue(prompt.contains("Variantes de selecao"));
        assertTrue(prompt.contains("Layout do ALV"));
    }

    @Test
    void testEnhancementPromptContainsExitCriteria() {
        String prompt = service.buildValidationPrompt("implementar BADI para validacao de documento");
        assertTrue(prompt.contains("ENHANCEMENT/EXIT/BADI"));
        assertTrue(prompt.contains("Nome tecnico EXATO do ponto de enhancement"));
    }

    @Test
    void testInterfacePromptContainsLandscapeCriteria() {
        String prompt = service.buildValidationPrompt("integracao RFC com sistema externo inbound");
        assertTrue(prompt.contains("INTERFACE/INTEGRACAO"));
        assertTrue(prompt.contains("Landscape completo"));
        assertTrue(prompt.contains("Procedimento de recuperacao"));
    }

    @Test
    void testUnknownTypeHasNoCriterioEspecifico() {
        String prompt = service.buildValidationPrompt("documento sem tipo identificavel");
        assertFalse(prompt.contains("CRITERIOS ESPECIFICOS"));
    }
}
