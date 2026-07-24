package com.company.specvalidator.service.validator;

import com.company.specvalidator.dto.ai.ChecklistItem;
import com.company.specvalidator.enums.ChecklistItemKey;
import com.company.specvalidator.enums.ChecklistStatus;
import com.company.specvalidator.enums.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreCalculatorTest {

    private ScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ScoreCalculator();
    }

    private ChecklistItem itemWith(ChecklistItemKey chave, ChecklistStatus status) {
        return ChecklistItem.builder()
                .chave(chave)
                .item("Item de teste")
                .status(status)
                .comentario("Comentario de teste")
                .build();
    }

    @Test
    void testPerfectScoreWithEmptyChecklist() {
        int score = calculator.calculateScore(Collections.emptyList());
        assertEquals(100, score, "Checklist vazio deve manter a pontuacao cheia de 100");
    }

    @Test
    void testAllItemsOkYieldsPerfectScore() {
        List<ChecklistItem> checklist = List.of(
                itemWith(ChecklistItemKey.DESCRICAO_PROCESSO, ChecklistStatus.OK),
                itemWith(ChecklistItemKey.REGRAS_NEGOCIO, ChecklistStatus.OK),
                itemWith(ChecklistItemKey.CONSISTENCIA, ChecklistStatus.OK)
        );
        assertEquals(100, calculator.calculateScore(checklist));
    }

    @Test
    void testParcialNonCriticalItemRemoves4Points() {
        List<ChecklistItem> checklist = List.of(itemWith(ChecklistItemKey.CASOS_USO, ChecklistStatus.PARCIAL));
        assertEquals(96, calculator.calculateScore(checklist));
    }

    @Test
    void testParcialCriticalItemAlsoRemovesOnly4Points() {
        // PARCIAL vale -4 independente do item ser da lista de criticos ou nao
        List<ChecklistItem> checklist = List.of(itemWith(ChecklistItemKey.REGRAS_NEGOCIO, ChecklistStatus.PARCIAL));
        assertEquals(96, calculator.calculateScore(checklist));
    }

    @Test
    void testAusenteNonCriticalItemRemoves6Points() {
        List<ChecklistItem> checklist = List.of(itemWith(ChecklistItemKey.CASOS_USO, ChecklistStatus.AUSENTE));
        assertEquals(94, calculator.calculateScore(checklist));
    }

    @Test
    void testAusenteCriticalItemRemoves10Points() {
        List<ChecklistItem> checklist = List.of(itemWith(ChecklistItemKey.REGRAS_NEGOCIO, ChecklistStatus.AUSENTE));
        assertEquals(90, calculator.calculateScore(checklist));
    }

    @Test
    void testAllFiveCriticalKeysAusenteEachCost10Points() {
        List<ChecklistItemKey> criticos = List.of(
                ChecklistItemKey.REGRAS_NEGOCIO,
                ChecklistItemKey.TRATAMENTO_EXCECOES,
                ChecklistItemKey.INPUTS_OUTPUTS,
                ChecklistItemKey.DEPENDENCIAS,
                ChecklistItemKey.CONDICOES_TESTE
        );
        List<ChecklistItem> checklist = criticos.stream()
                .map(chave -> itemWith(chave, ChecklistStatus.AUSENTE))
                .toList();
        // 5 itens criticos ausentes: 100 - 5*10 = 50
        assertEquals(50, calculator.calculateScore(checklist));
    }

    @Test
    void testMensagensValidacoesAusenteIsNotCriticalCosts6Points() {
        // "mensagens_validacoes" NAO esta na lista de criticos (ver comentario em ScoreCalculator) —
        // por falta de confirmacao com o Marcio sobre o 6o item ("erro") da regra original.
        List<ChecklistItem> checklist = List.of(itemWith(ChecklistItemKey.MENSAGENS_VALIDACOES, ChecklistStatus.AUSENTE));
        assertEquals(94, calculator.calculateScore(checklist));
    }

    @Test
    void testScoreCanBeNegativeForVeryBadEfs() {
        // Os 16 itens do checklist, todos AUSENTE: 5 criticos * -10 + 11 nao-criticos * -6 = -116
        List<ChecklistItem> checklist = java.util.EnumSet.allOf(ChecklistItemKey.class).stream()
                .map(chave -> itemWith(chave, ChecklistStatus.AUSENTE))
                .toList();
        assertEquals(-16, calculator.calculateScore(checklist),
                "16/16 itens AUSENTE deve resultar em 100 - 116 = -16 (sem floor)");
    }

    @Test
    void testCalculatePontosOkIsZero() {
        assertEquals(0, calculator.calculatePontos(itemWith(ChecklistItemKey.CONSISTENCIA, ChecklistStatus.OK)));
    }

    @Test
    void testCalculatePontosParcialIsMinus4() {
        assertEquals(-4, calculator.calculatePontos(itemWith(ChecklistItemKey.CONSISTENCIA, ChecklistStatus.PARCIAL)));
    }

    @Test
    void testCalculatePontosAusenteNonCriticalIsMinus6() {
        assertEquals(-6, calculator.calculatePontos(itemWith(ChecklistItemKey.CONSISTENCIA, ChecklistStatus.AUSENTE)));
    }

    @Test
    void testCalculatePontosAusenteCriticalIsMinus10() {
        assertEquals(-10, calculator.calculatePontos(itemWith(ChecklistItemKey.DEPENDENCIAS, ChecklistStatus.AUSENTE)));
    }

    @Test
    void testClassificacaoReprovadoAtOrBelow39() {
        assertEquals(ValidationStatus.REPROVADO, calculator.calculateClassificacao(39));
        assertEquals(ValidationStatus.REPROVADO, calculator.calculateClassificacao(0));
        assertEquals(ValidationStatus.REPROVADO, calculator.calculateClassificacao(-50));
    }

    @Test
    void testClassificacaoAceitavelBetween40And60() {
        assertEquals(ValidationStatus.ACEITAVEL, calculator.calculateClassificacao(40));
        assertEquals(ValidationStatus.ACEITAVEL, calculator.calculateClassificacao(50));
        assertEquals(ValidationStatus.ACEITAVEL, calculator.calculateClassificacao(60));
    }

    @Test
    void testClassificacaoAprovadoAbove60() {
        assertEquals(ValidationStatus.APROVADO, calculator.calculateClassificacao(61));
        assertEquals(ValidationStatus.APROVADO, calculator.calculateClassificacao(100));
    }
}
