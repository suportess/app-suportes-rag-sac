package com.company.specvalidator.service.validator;

import com.company.specvalidator.dto.ai.ChecklistItem;
import com.company.specvalidator.enums.ChecklistItemKey;
import com.company.specvalidator.enums.ChecklistStatus;
import com.company.specvalidator.enums.ValidationStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class ScoreCalculator {

    // Itens cuja ausencia impede o inicio do desenvolvimento (conforme modelo do Marcio:
    // regras de negocio, exceções, inputs/outputs, dependencias, teste). O texto original dele
    // citava um 6o item ("erro") sem nome literal entre os 16 criterios — como nao ha
    // confirmacao de qual criterio isso seria, optamos por nao incluir nenhum item por suposicao;
    // mensagens_validacoes fica no -6 padrao (AUSENTE nao-critico) ate confirmacao com o Marcio.
    private static final Set<ChecklistItemKey> ITENS_CRITICOS = Set.of(
            ChecklistItemKey.REGRAS_NEGOCIO,
            ChecklistItemKey.TRATAMENTO_EXCECOES,
            ChecklistItemKey.INPUTS_OUTPUTS,
            ChecklistItemKey.DEPENDENCIAS,
            ChecklistItemKey.CONDICOES_TESTE
    );

    public int calculatePontos(ChecklistItem item) {
        if (item.getStatus() == ChecklistStatus.OK) {
            return 0;
        }
        if (item.getStatus() == ChecklistStatus.PARCIAL) {
            return -4;
        }
        // AUSENTE
        return ITENS_CRITICOS.contains(item.getChave()) ? -10 : -6;
    }

    public int calculateScore(List<ChecklistItem> checklist) {
        int score = 100;
        for (ChecklistItem item : checklist) {
            score += calculatePontos(item);
        }
        return score; // sem piso — checklist com muitos gaps pode ficar negativo
    }

    public ValidationStatus calculateClassificacao(int score) {
        if (score <= 39) {
            return ValidationStatus.REPROVADO;
        }
        if (score <= 60) {
            return ValidationStatus.ACEITAVEL;
        }
        return ValidationStatus.APROVADO;
    }
}
