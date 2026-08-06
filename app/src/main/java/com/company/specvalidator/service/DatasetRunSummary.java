package com.company.specvalidator.service;

import com.company.specvalidator.dto.ai.PontoCritico;

import java.util.List;
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
        String resumoExecutivo,
        String checklist,
        String parecerFinal,
        List<String> recomendacoes,
        List<PontoCritico> pontosCriticos,
        Map<String, Object> expectedOutput,
        String traceId
) {
}
