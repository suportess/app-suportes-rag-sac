package com.company.specvalidator.service;

public record ItemDiff(String itemLabel, String classificacaoBaseline, Integer scoreBaseline,
                       String classificacaoAtual, Integer scoreAtual) {
}
