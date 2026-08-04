package com.company.specvalidator.service;

/**
 * Resultado da validacao de um item de Dataset da Langfuse rodado pelo pipeline real.
 */
public record DatasetRunItemResult(Integer score, String classificacao, String qualidade) {
}
