package com.company.specvalidator.service;

import java.util.Map;

/**
 * Resumo de um item de Dataset da Langfuse rodado pelo pipeline real de validacao —
 * retornado pelo endpoint de dataset run pra comparacao rapida sem precisar abrir a Langfuse.
 */
public record DatasetRunSummary(
        String itemId,
        String itemLabel,
        Integer score,
        String classificacao,
        String qualidade,
        Map<String, Object> expectedOutput,
        String traceId
) {
}
