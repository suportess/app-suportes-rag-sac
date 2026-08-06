package com.company.specvalidator.service;

import com.company.specvalidator.dto.ai.PontoCritico;

import java.util.List;

/**
 * Resultado da validacao de um item de Dataset da Langfuse rodado pelo pipeline real.
 */
public record DatasetRunItemResult(
        Integer score, 
        String classificacao, 
        String qualidade, 
        String resumoExecutivo, 
        String checklist, 
        String parecerFinal,
        List<String> recomendacoes,
        List<PontoCritico> pontosCriticos) {
}
