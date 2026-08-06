package com.company.specvalidator.service;

import java.util.List;

/**
 * Nota de consistencia calculada a partir de N execucoes repetidas do mesmo item de dataset
 * (mesmo texto de EF, mesmo prompt). Agrupamento feito por itemLabel — dataset precisa ter as N
 * linhas repetidas com o mesmo metadata.nome pra isso funcionar.
 */
public record ConsistencyResult(
        String itemLabel,
        int execucoes,
        List<Integer> scores,
        double media,
        double desvioPadrao,
        double taxaEstabilidadeClassificacao,
        String classificacaoMaisComum
) {
}
