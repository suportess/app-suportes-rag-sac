package com.company.specvalidator.service;

import java.util.List;

/**
 * Resultado completo de uma rodada de Dataset Run: os itens individuais + a nota de
 * consistencia calculada pra grupos de itens repetidos (mesmo itemLabel, N execucoes).
 */
public record DatasetRunResult(List<DatasetRunSummary> items, List<ConsistencyResult> consistency) {
}
