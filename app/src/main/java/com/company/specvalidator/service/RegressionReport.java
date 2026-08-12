package com.company.specvalidator.service;

import java.util.List;

/**
 * Compara os resultados de um Dataset Run contra o run marcado como baseline pro mesmo dataset —
 * mostra quantos itens mantiveram a mesma classificacao e detalha os que mudaram, correlacionando
 * com a versao de prompt de cada lado pra facilitar achar em qual mudanca a regressao comecou.
 */
public record RegressionReport(String datasetName, String baselineRunName, String currentRunName,
                               Integer baselinePromptVersion, Integer currentPromptVersion,
                               int totalItensComparados, int itensIguaisAoBaseline,
                               double percentualEstabilidadeVsBaseline, List<ItemDiff> mudancas) {
}
