package com.company.specvalidator.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.company.specvalidator.enums.DevType.*;
import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderServiceTest {

    private PromptBuilderService service;

    @BeforeEach
    void setUp() {
        service = new PromptBuilderService();
    }

        private void assertCriterionUsesDetailedStructure(String prompt, String criterionTitle) {
        int start = prompt.indexOf(criterionTitle);
        assertTrue(start >= 0, "Prompt deveria conter o criterio: " + criterionTitle);

        int end = Math.min(prompt.length(), start + 900);
        String criterionBlock = prompt.substring(start, end);

        assertTrue(criterionBlock.contains("Como avaliar: REGRA OBJETIVA -"),
            "Criterio deveria conter 'Como avaliar: REGRA OBJETIVA -': " + criterionTitle);
        assertTrue(criterionBlock.contains("Classifique como OK quando"),
            "Criterio deveria detalhar classificacao OK: " + criterionTitle);
        assertTrue(criterionBlock.contains("Classifique como Parcial quando"),
            "Criterio deveria detalhar classificacao Parcial: " + criterionTitle);
        assertTrue(criterionBlock.contains("Classifique como Ausente quando"),
            "Criterio deveria detalhar classificacao Ausente: " + criterionTitle);
        }

    // --- prompt structure ---

    @Test
    void testUserPromptContainsDocumentText() {
        String documentText = "Minha especificacao funcional SAP ABAP para modulo MM.";
        String userPrompt = service.buildUserPrompt(documentText);
        assertTrue(userPrompt.contains(documentText));
    }

    @Test
    void testSystemPromptDoesNotContainDocumentText() {
        String documentText = "UNIQUE_DOCUMENT_MARKER_12345";
        String systemPrompt = service.buildSystemPrompt(documentText);
        assertFalse(systemPrompt.contains(documentText));
    }

    @Test
    void testBuildSystemPromptOverloadAcceptsDevTypeDirectly() {
        // evita detectar duas vezes: ValidationAgentService detecta uma vez e passa pro prompt
        String prompt = service.buildSystemPrompt("qualquer texto", TABLE);
        assertTrue(prompt.contains(TABLE.displayName()));
    }

    @Test
    void testPromptContainsJsonFormat() {
        String prompt = service.buildSystemPrompt("qualquer texto");
        assertTrue(prompt.contains("JSON"));
        assertTrue(prompt.contains("\"qualidade\""));
        assertTrue(prompt.contains("\"checklist\""));
        assertTrue(prompt.contains("\"chave\""));
        assertTrue(prompt.contains("\"pontosCriticos\""));
        assertTrue(prompt.contains("\"recomendacoes\""));
        assertTrue(prompt.contains("\"parecerFinal\""));
    }

    @Test
    void testPromptDoesNotAskAiForScoreOrClassificacaoFields() {
        // score/classificacao sao calculados pelo backend a partir do checklist, nao pela IA
        String prompt = service.buildSystemPrompt("qualquer texto");
        assertTrue(prompt.contains("NAO inclua os campos \"score\" nem \"classificacao\""));
    }

    @Test
    void testPromptContainsChecklistStatusValues() {
        String prompt = service.buildSystemPrompt("qualquer texto");
        assertTrue(prompt.contains("NAO exija perfeicao ou"));
        assertTrue(prompt.contains("Ausente = informacao totalmente inexistente dentro do documento"));
    }

    @Test
    void testPromptContainsGeneralCriteria() {
        String prompt = service.buildSystemPrompt("qualquer texto");
        assertTrue(prompt.contains("Objetivo e escopo"));
        assertTrue(prompt.contains("Regras de negocio"));
        assertTrue(prompt.contains("Condicoes de teste"));
    }

    @Test
    void testUpdatedCriteriaUseDetailedStructureForUnknownTypePrompt() {
        String prompt = service.buildSystemPrompt("documento generico sem palavras chave especificas", UNKNOWN);

        String[] criteria = {
                "Descricao do processo (chave: descricao_processo)",
                "Objetivo e escopo (chave: objetivo_escopo)",
                "Regras de negocio (chave: regras_negocio)",
                "Tratamento de excecoes (chave: tratamento_excecoes)",
                "Inputs e outputs (chave: inputs_outputs)",
                "Controle de acesso / autorizacoes (chave: controle_acesso)",
                "Logs, rastreabilidade e reprocessamento/recuperacao (chave: logs_reprocessamento)",
                "Mensagens e validacoes (chave: mensagens_validacoes)",
                "Condicoes de teste (chave: condicoes_teste)"
        };

        for (String criterion : criteria) {
            assertCriterionUsesDetailedStructure(prompt, criterion);
        }
    }

    @Test
    void testUpdatedConditionalCriteriaUseDetailedStructureWhenApplicable() {
        String tablePrompt = service.buildSystemPrompt("nova tabela z com dicionario abap e chave primaria definida", TABLE);
        assertCriterionUsesDetailedStructure(tablePrompt,
                "Campos e estrutura de dados — origem, tipo, tamanho, formato, dominio/range, obrigatoriedade (chave: campos_estrutura_dados)");

        String reportPrompt = service.buildSystemPrompt("relatorio ALV com execucao diaria e dependencia de BAPI", REPORT);
        assertCriterionUsesDetailedStructure(reportPrompt,
                "Dependencias — integracoes, tabelas SAP, programas predecessores, servicos, arquivos, sistemas externos (chave: dependencias)");
        assertCriterionUsesDetailedStructure(reportPrompt,
                "Volume de dados e frequencia de execucao (chave: volume_frequencia)");
    }

    // --- criterios "obrigatorios" (9) sempre presentes, independente do tipo ---

    @Test
    void testCoreCriteriaAlwaysPresentEvenForUnknownType() {
        String prompt = service.buildSystemPrompt("documento generico sem palavras chave especificas");
        String[] chavesObrigatorias = {
                "descricao_processo", "objetivo_escopo", "casos_uso", "fluxos_alternativos",
                "regras_negocio", "tratamento_excecoes", "inputs_outputs", "condicoes_teste",
                "massa_dados"
        };
        for (String chave : chavesObrigatorias) {
            assertTrue(prompt.contains(chave), "Prompt deveria conter a chave obrigatoria: " + chave);
        }
    }

    @Test
    void testConsistenciaIsNotAChecklistItemAnymore() {
        // consistencia saiu do checklist pontuado e virou regra transversal (item 6 das regras obrigatorias)
        String prompt = service.buildSystemPrompt("qualquer texto");
        assertFalse(prompt.contains("chave: consistencia"));
        assertTrue(prompt.contains("6. CONSISTENCIA"));
    }

    // --- criterios "condicionais" (6) so aparecem quando o DevType habilita ---

    @Test
    void testConditionalCriteriaAlwaysPresentRegardlessOfType() {
        // controle_acesso, logs_reprocessamento e mensagens_validacoes valem pra "todos os tipos WRICEF"
        String prompt = service.buildSystemPrompt("documento generico sem palavras chave especificas");
        assertTrue(prompt.contains("chave: controle_acesso"));
        assertTrue(prompt.contains("chave: logs_reprocessamento"));
        assertTrue(prompt.contains("chave: mensagens_validacoes"));
    }

    @Test
    void testCamposEstruturaDadosOnlyAppearsForTableType() {
        String promptTable = service.buildSystemPrompt("nova tabela z com dicionario abap e chave primaria definida", TABLE);
        assertTrue(promptTable.contains("chave: campos_estrutura_dados"));

        String promptUnknown = service.buildSystemPrompt("documento generico sem palavras chave especificas", UNKNOWN);
        assertFalse(promptUnknown.contains("chave: campos_estrutura_dados"));
    }

    @Test
    void testDependenciasOnlyAppearsForReportInterfaceOuBatch() {
        String promptReport = service.buildSystemPrompt("relatorio ALV", REPORT);
        assertTrue(promptReport.contains("chave: dependencias"));

        String promptEnhancement = service.buildSystemPrompt("implementar BADI para validacao", ENHANCEMENT);
        assertFalse(promptEnhancement.contains("chave: dependencias"));
    }

    @Test
    void testVolumeFrequenciaOnlyAppearsForReportInterfaceBatchOuForms() {
        String promptForms = service.buildSystemPrompt("formulario SmartForm", FORMS);
        assertTrue(promptForms.contains("chave: volume_frequencia"));

        String promptTable = service.buildSystemPrompt("nova tabela z", TABLE);
        assertFalse(promptTable.contains("chave: volume_frequencia"));
    }

    // --- exemplos (chain-of-thought removido do prompt, mantido so nos exemplos) ---

    @Test
    void testPromptContainsAnaliseExamplesWithoutChainOfThought() {
        String prompt = service.buildSystemPrompt("qualquer texto");
        // exemplos ainda mostram o raciocinio esperado (marcador ANALISE), mas o
        // bloco de scratchpad RACIOCINIO/<metodo_cot> foi removido do prompt
        assertTrue(prompt.contains("ANALISE ["));
        assertTrue(prompt.contains("Conclusao:"));
        assertFalse(prompt.contains("RACIOCINIO ["));
        assertFalse(prompt.contains("<metodo_cot>"));
    }

    @Test
    void testPromptForbidsChainOfThoughtFieldInJson() {
        String prompt = service.buildSystemPrompt("qualquer texto");
        // the field name may appear only in the PROHIBITION instruction, never as a schema field
        assertTrue(prompt.contains("PROIBIDO incluir o campo \"chainOfThought_Analysis\""));
    }

    @Test
    void testPromptContainsMandatoryRules() {
        String prompt = service.buildSystemPrompt("qualquer texto");
        assertTrue(prompt.contains("NAO ASSUMIR INFORMACOES E DADOS"));
        assertTrue(prompt.contains("NAO SEJA GENERICO"));
        assertTrue(prompt.contains("FOCO NA EXECUTABILIDADE"));
        assertTrue(prompt.contains("CLASSIFICACAO RIGIDA"));
        assertTrue(prompt.contains("DETECÇAO DE RISCO"));
        assertTrue(prompt.contains("6. CONSISTENCIA"));
    }

    @Test
    void testPromptContainsExpectedBehaviorSection() {
        String prompt = service.buildSystemPrompt("qualquer texto");
        assertTrue(prompt.contains("Seja direto"));
        assertTrue(prompt.contains("Seja critico"));
        assertTrue(prompt.contains("Nao suavize os problemas encontrados"));
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
    void testDetectsArquivoType() {
        assertEquals(ARQUIVO, service.detectDevType("arquivo plano com layout de arquivo definido via ftp"));
        assertEquals(ARQUIVO, service.detectDevType("arquivo texto com separador de colunas e estrutura de arquivo"));
    }

    @Test
    void testDetectsTelaFioriType() {
        assertEquals(TELA_FIORI, service.detectDevType("aplicativo fiori com tile no launchpad"));
        assertEquals(TELA_FIORI, service.detectDevType("desenvolvimento em sapui5 para nova tela customizada"));
    }

    @Test
    void testReturnsUnknownWhenNoSignificantKeywords() {
        assertEquals(UNKNOWN, service.detectDevType("documento generico sem palavras chave especificas"));
        assertEquals(UNKNOWN, service.detectDevType(""));
        assertEquals(UNKNOWN, service.detectDevType(null));
    }

    // --- type-specific criteria in prompt ---

    @Test
    void testReportPromptContainsSpecificCriteria() {
        String prompt = service.buildSystemPrompt("relatorio ALV para exibir dados de material");
        assertTrue(prompt.contains("CRITERIOS ESPECIFICOS: REPORT"));
        assertTrue(prompt.contains("Variantes de selecao"));
        assertTrue(prompt.contains("Colunas do relatorio"));
    }

    @Test
    void testEnhancementPromptContainsExitCriteria() {
        String prompt = service.buildSystemPrompt("implementar BADI para validacao de documento");
        assertTrue(prompt.contains("CRITERIOS ESPECIFICOS: ENHANCEMENT"));
        assertTrue(prompt.contains("Nome tecnico EXATO do ponto de enhancement"));
    }

    @Test
    void testInterfacePromptContainsWricefCriteria() {
        String prompt = service.buildSystemPrompt("integracao RFC com sistema externo inbound");
        assertTrue(prompt.contains("CRITERIOS ESPECIFICOS: INTERFACE / PI-CPI"));
        assertTrue(prompt.contains("Direcao definida (inbound/outbound)"));
        assertTrue(prompt.contains("Senders/receivers"));
    }

    @Test
    void testArquivoPromptContainsWricefCriteria() {
        String prompt = service.buildSystemPrompt("arquivo plano via ftp com layout de arquivo definido");
        assertTrue(prompt.contains("CRITERIOS ESPECIFICOS: ARQUIVO"));
        assertTrue(prompt.contains("Exemplo de arquivo fornecido"));
    }

    @Test
    void testUnknownTypeHasNoCriterioEspecifico() {
        String prompt = service.buildSystemPrompt("documento sem tipo identificavel");
        assertFalse(prompt.contains("CRITERIOS ESPECIFICOS"));
    }
}
