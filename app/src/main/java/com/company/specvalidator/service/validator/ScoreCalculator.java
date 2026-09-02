package com.company.specvalidator.service.validator;

import com.company.specvalidator.config.ScoringConfig.ScoringProperties;
import com.company.specvalidator.dto.ai.ChecklistItem;
import com.company.specvalidator.enums.ChecklistItemKey;
import com.company.specvalidator.enums.DevType;
import com.company.specvalidator.enums.ValidationStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ScoreCalculator {

    // Criterios "obrigatorios" (definidos pelo negocio em 2026-07-27/28): sempre contam pro
    // score, independente do tipo de desenvolvimento identificado.
    private static final Set<ChecklistItemKey> ITENS_OBRIGATORIOS = EnumSet.of(
            ChecklistItemKey.DESCRICAO_PROCESSO,
            ChecklistItemKey.OBJETIVO_ESCOPO,
            ChecklistItemKey.CASOS_USO,
            ChecklistItemKey.FLUXOS_ALTERNATIVOS,
            ChecklistItemKey.REGRAS_NEGOCIO,
            ChecklistItemKey.TRATAMENTO_EXCECOES,
            ChecklistItemKey.INPUTS_OUTPUTS,
            ChecklistItemKey.CONDICOES_TESTE,
            ChecklistItemKey.MASSA_DADOS
    );

    // Criterios "condicionais": so contam pro score quando o DevType detectado esta no conjunto
    // habilitado (mapa definido pelo negocio a partir das siglas WRICEF que eles passaram na
    // reuniao de 2026-07-27/28 — ex: Dependencias so pra Report/Interface/Conversao).
    private static final Map<ChecklistItemKey, Set<DevType>> APLICABILIDADE_CONDICIONAIS = Map.of(
            ChecklistItemKey.CAMPOS_ESTRUTURA_DADOS, EnumSet.of(DevType.TABLE),
            ChecklistItemKey.DEPENDENCIAS, EnumSet.of(DevType.REPORT, DevType.INTERFACE, DevType.BATCH),
            ChecklistItemKey.CONTROLE_ACESSO, EnumSet.allOf(DevType.class),
            ChecklistItemKey.VOLUME_FREQUENCIA, EnumSet.of(DevType.REPORT, DevType.INTERFACE, DevType.BATCH, DevType.FORMS),
            ChecklistItemKey.LOGS_REPROCESSAMENTO, EnumSet.allOf(DevType.class),
            ChecklistItemKey.MENSAGENS_VALIDACOES, EnumSet.allOf(DevType.class)
    );

    private final ScoringProperties scoringProperties;

    public ScoreCalculator(ScoringProperties scoringProperties) {
        this.scoringProperties = scoringProperties;
    }

    public boolean isApplicable(ChecklistItemKey chave, DevType devType) {
        if (ITENS_OBRIGATORIOS.contains(chave)) {
            return true;
        }
        Set<DevType> tiposHabilitados = APLICABILIDADE_CONDICIONAIS.get(chave);
        return tiposHabilitados != null && tiposHabilitados.contains(devType);
    }

    /**
     * Score proporcional: score = (conquistado / possivel) * 100.
     * OK ganha o peso cheio do criterio, Parcial ganha uma fracao configuravel (padrao 50%,
     * ver app.scoring.parcial-multiplier), Ausente ganha zero. So os criterios aplicaveis ao
     * DevType entram na conta de "possivel" — por isso uma EF vazia sempre da 0 (conquistado=0),
     * diferente do modelo antigo de "desconta de 100" que deixava um piso artificial.
     */
    public int calculateScore(List<ChecklistItem> checklist, DevType devType) {
        List<ChecklistItem> aplicaveis = checklist.stream()
                .filter(item -> isApplicable(item.getChave(), devType))
                .toList();

        int possivel = aplicaveis.stream().mapToInt(item -> pesoDe(item.getChave())).sum();
        if (possivel == 0) {
            return 0;
        }

        double conquistado = aplicaveis.stream().mapToDouble(this::pontosConquistados).sum();
        return (int) Math.round((conquistado / possivel) * 100);
    }

    public ValidationStatus calculateClassificacao(int score) {
        if (score <= 39) {
            return ValidationStatus.REPROVADO;
        }
        if (score <= 69) {
            return ValidationStatus.ACEITAVEL;
        }
        return ValidationStatus.APROVADO;
    }

    /**
     * Usado so pro demonstrativo na tela (coluna "Valor") — quantos pontos do peso do criterio
     * foram perdidos, arredondado pro inteiro mais proximo. E uma visualizacao aproximada por
     * item; a base real do score e o calculo fracionado de {@link #calculateScore}.
     */
    public int calculatePontosPerdidos(ChecklistItem item) {
        int peso = pesoDe(item.getChave());
        double conquistado = pontosConquistados(item);
        return (int) Math.round(conquistado - peso);
    }

    /**
     * Pontos conquistados (fracao exata, sem arredondar) por esse item — usado pro demonstrativo
     * transparente na tela (ex: "1.5 de 3"), em vez do valor de perda ja arredondado.
     */
    public double pontosConquistados(ChecklistItem item) {
        int peso = pesoDe(item.getChave());
        return switch (item.getStatus()) {
            case OK -> peso;
            case PARCIAL -> peso * scoringProperties.getParcialMultiplier();
            case AUSENTE -> 0;
        };
    }

    /**
     * Peso (1-5, definido pelo negocio) desse criterio — usado pro demonstrativo transparente
     * na tela, pra mostrar "quanto valia" cada item alem do quanto foi conquistado.
     */
    public int pesoDe(ChecklistItemKey chave) {
        String jsonKey = chave.name().toLowerCase(Locale.ROOT);
        Integer peso = scoringProperties.getPesos().get(jsonKey);
        if (peso == null) {
            throw new IllegalStateException("Peso nao configurado para o criterio: " + jsonKey);
        }
        return peso;
    }
}
