package com.company.specvalidator.service.validator;

import com.company.specvalidator.config.ScoringConfig.ScoringProperties;
import com.company.specvalidator.dto.ai.ChecklistItem;
import com.company.specvalidator.enums.ChecklistItemKey;
import com.company.specvalidator.enums.ChecklistStatus;
import com.company.specvalidator.enums.DevType;
import com.company.specvalidator.enums.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScoreCalculatorTest {

    private ScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        ScoringProperties props = new ScoringProperties();
        props.setParcialMultiplier(0.5);
        Map<String, Integer> pesos = new LinkedHashMap<>();
        pesos.put("descricao_processo", 3);
        pesos.put("objetivo_escopo", 4);
        pesos.put("casos_uso", 4);
        pesos.put("fluxos_alternativos", 5);
        pesos.put("regras_negocio", 5);
        pesos.put("tratamento_excecoes", 3);
        pesos.put("inputs_outputs", 4);
        pesos.put("condicoes_teste", 4);
        pesos.put("massa_dados", 5);
        pesos.put("campos_estrutura_dados", 3);
        pesos.put("dependencias", 3);
        pesos.put("controle_acesso", 3);
        pesos.put("volume_frequencia", 3);
        pesos.put("logs_reprocessamento", 3);
        pesos.put("mensagens_validacoes", 3);
        props.setPesos(pesos);
        calculator = new ScoreCalculator(props);
    }

    private ChecklistItem itemWith(ChecklistItemKey chave, ChecklistStatus status) {
        return ChecklistItem.builder()
                .chave(chave)
                .item("Item de teste")
                .status(status)
                .comentario("Comentario de teste")
                .build();
    }

    // --- isApplicable (pesos e aplicabilidade definidos pelo negocio em 2026-07-27/28) ---

    @Test
    void testCoreItemsAlwaysApplicableRegardlessOfType() {
        assertTrue(calculator.isApplicable(ChecklistItemKey.REGRAS_NEGOCIO, DevType.UNKNOWN));
        assertTrue(calculator.isApplicable(ChecklistItemKey.MASSA_DADOS, DevType.TABLE));
    }

    @Test
    void testCamposEstruturaDadosOnlyApplicableForTable() {
        assertTrue(calculator.isApplicable(ChecklistItemKey.CAMPOS_ESTRUTURA_DADOS, DevType.TABLE));
        assertFalse(calculator.isApplicable(ChecklistItemKey.CAMPOS_ESTRUTURA_DADOS, DevType.UNKNOWN));
        assertFalse(calculator.isApplicable(ChecklistItemKey.CAMPOS_ESTRUTURA_DADOS, DevType.REPORT));
    }

    @Test
    void testDependenciasApplicableOnlyForReportInterfaceBatch() {
        assertTrue(calculator.isApplicable(ChecklistItemKey.DEPENDENCIAS, DevType.REPORT));
        assertTrue(calculator.isApplicable(ChecklistItemKey.DEPENDENCIAS, DevType.INTERFACE));
        assertTrue(calculator.isApplicable(ChecklistItemKey.DEPENDENCIAS, DevType.BATCH));
        assertFalse(calculator.isApplicable(ChecklistItemKey.DEPENDENCIAS, DevType.ENHANCEMENT));
        assertFalse(calculator.isApplicable(ChecklistItemKey.DEPENDENCIAS, DevType.UNKNOWN));
    }

    @Test
    void testControleAcessoLogsMensagensApplicableForAllTypesIncludingUnknown() {
        for (DevType tipo : DevType.values()) {
            assertTrue(calculator.isApplicable(ChecklistItemKey.CONTROLE_ACESSO, tipo), tipo + " deveria habilitar controle_acesso");
            assertTrue(calculator.isApplicable(ChecklistItemKey.LOGS_REPROCESSAMENTO, tipo), tipo + " deveria habilitar logs_reprocessamento");
            assertTrue(calculator.isApplicable(ChecklistItemKey.MENSAGENS_VALIDACOES, tipo), tipo + " deveria habilitar mensagens_validacoes");
        }
    }

    @Test
    void testVolumeFrequenciaApplicableForReportInterfaceBatchForms() {
        assertTrue(calculator.isApplicable(ChecklistItemKey.VOLUME_FREQUENCIA, DevType.FORMS));
        assertFalse(calculator.isApplicable(ChecklistItemKey.VOLUME_FREQUENCIA, DevType.TABLE));
        assertFalse(calculator.isApplicable(ChecklistItemKey.VOLUME_FREQUENCIA, DevType.ENHANCEMENT));
    }

    // --- calculateScore (modelo proporcional: conquistado / possivel * 100) ---

    @Test
    void testEmptyChecklistReturnsZero() {
        assertEquals(0, calculator.calculateScore(List.of(), DevType.UNKNOWN));
    }

    @Test
    void testAllOkYieldsScore100() {
        List<ChecklistItem> checklist = List.of(
                itemWith(ChecklistItemKey.DESCRICAO_PROCESSO, ChecklistStatus.OK),
                itemWith(ChecklistItemKey.REGRAS_NEGOCIO, ChecklistStatus.OK),
                itemWith(ChecklistItemKey.MASSA_DADOS, ChecklistStatus.OK)
        );
        assertEquals(100, calculator.calculateScore(checklist, DevType.UNKNOWN));
    }

    @Test
    void testAllAusenteYieldsScoreZeroEvenWithManyCriteriaApplicable() {
        // Teste de sanidade que motivou a troca de modelo (2026-07-28): uma EF totalmente vazia
        // tem que dar 0, nao importa quantos criterios se aplicam ao tipo — diferente do modelo
        // antigo de "desconta de 100", que deixava um piso artificial (45 a 63).
        List<ChecklistItem> checklist = EnumSet.allOf(ChecklistItemKey.class).stream()
                .map(chave -> itemWith(chave, ChecklistStatus.AUSENTE))
                .toList();
        assertEquals(0, calculator.calculateScore(checklist, DevType.REPORT));
    }

    @Test
    void testAllParcialYieldsHalfScoreRegardlessOfWeights() {
        // Parcial sempre ganha 50% do peso — entao se TUDO for Parcial, o score e sempre 50,
        // independente de quais pesos especificos estao envolvidos.
        List<ChecklistItem> checklist = List.of(
                itemWith(ChecklistItemKey.DESCRICAO_PROCESSO, ChecklistStatus.PARCIAL),
                itemWith(ChecklistItemKey.REGRAS_NEGOCIO, ChecklistStatus.PARCIAL),
                itemWith(ChecklistItemKey.MASSA_DADOS, ChecklistStatus.PARCIAL)
        );
        assertEquals(50, calculator.calculateScore(checklist, DevType.UNKNOWN));
    }

    @Test
    void testMixedStatusComputesProportionally() {
        // regras_negocio (peso 5) OK, massa_dados (peso 5) Ausente, descricao_processo (peso 3) Parcial
        // possivel = 5+5+3 = 13; conquistado = 5 + 0 + 1.5 = 6.5; score = 6.5/13*100 = 50
        List<ChecklistItem> checklist = List.of(
                itemWith(ChecklistItemKey.REGRAS_NEGOCIO, ChecklistStatus.OK),
                itemWith(ChecklistItemKey.MASSA_DADOS, ChecklistStatus.AUSENTE),
                itemWith(ChecklistItemKey.DESCRICAO_PROCESSO, ChecklistStatus.PARCIAL)
        );
        assertEquals(50, calculator.calculateScore(checklist, DevType.UNKNOWN));
    }

    @Test
    void testConditionalItemsNotApplicableToTypeAreExcludedFromCalculation() {
        // campos_estrutura_dados (peso 3) so aplica pra TABLE. Pra um REPORT, mesmo que venha
        // Ausente no checklist, nao deveria contar nem no possivel nem no conquistado.
        List<ChecklistItem> checklist = List.of(
                itemWith(ChecklistItemKey.REGRAS_NEGOCIO, ChecklistStatus.OK),
                itemWith(ChecklistItemKey.CAMPOS_ESTRUTURA_DADOS, ChecklistStatus.AUSENTE)
        );
        // Se contasse: possivel=5+3=8, conquistado=5 -> score=62. Como nao se aplica a REPORT,
        // so regras_negocio conta: possivel=5, conquistado=5 -> score=100.
        assertEquals(100, calculator.calculateScore(checklist, DevType.REPORT));
    }

    // --- calculateClassificacao ---

    @Test
    void testClassificacaoReprovadoAtOrBelow39() {
        assertEquals(ValidationStatus.REPROVADO, calculator.calculateClassificacao(39));
        assertEquals(ValidationStatus.REPROVADO, calculator.calculateClassificacao(0));
    }

    @Test
    void testClassificacaoAceitavelBetween40And60() {
        assertEquals(ValidationStatus.ACEITAVEL, calculator.calculateClassificacao(40));
        assertEquals(ValidationStatus.ACEITAVEL, calculator.calculateClassificacao(69));
    }

    @Test
    void testClassificacaoAprovadoAbove60() {
        assertEquals(ValidationStatus.APROVADO, calculator.calculateClassificacao(70));
        assertEquals(ValidationStatus.APROVADO, calculator.calculateClassificacao(100));
    }

    // --- calculatePontosPerdidos (coluna "Valor" do demonstrativo na tela) ---

    @Test
    void testPontosPerdidosOkIsZero() {
        assertEquals(0, calculator.calculatePontosPerdidos(itemWith(ChecklistItemKey.REGRAS_NEGOCIO, ChecklistStatus.OK)));
    }

    @Test
    void testPontosPerdidosAusenteIsFullWeightNegative() {
        assertEquals(-5, calculator.calculatePontosPerdidos(itemWith(ChecklistItemKey.REGRAS_NEGOCIO, ChecklistStatus.AUSENTE)));
    }

    @Test
    void testPontosPerdidosParcialIsHalfWeightNegative() {
        // peso 4 (objetivo_escopo), parcial ganha 2, perde 2
        assertEquals(-2, calculator.calculatePontosPerdidos(itemWith(ChecklistItemKey.OBJETIVO_ESCOPO, ChecklistStatus.PARCIAL)));
    }

    // --- pesoDe e pontosConquistados (demonstrativo transparente: peso + fracao) ---

    @Test
    void testPesoDeReturnsConfiguredWeight() {
        assertEquals(5, calculator.pesoDe(ChecklistItemKey.REGRAS_NEGOCIO));
        assertEquals(3, calculator.pesoDe(ChecklistItemKey.DESCRICAO_PROCESSO));
    }

    @Test
    void testPontosConquistadosOkIsFullWeight() {
        assertEquals(5.0, calculator.pontosConquistados(itemWith(ChecklistItemKey.REGRAS_NEGOCIO, ChecklistStatus.OK)));
    }

    @Test
    void testPontosConquistadosParcialIsHalfWeight() {
        // peso 3 (descricao_processo), parcial = 50% = 1.5
        assertEquals(1.5, calculator.pontosConquistados(itemWith(ChecklistItemKey.DESCRICAO_PROCESSO, ChecklistStatus.PARCIAL)));
    }

    @Test
    void testPontosConquistadosAusenteIsZero() {
        assertEquals(0.0, calculator.pontosConquistados(itemWith(ChecklistItemKey.MASSA_DADOS, ChecklistStatus.AUSENTE)));
    }
}
