package com.company.specvalidator.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum ChecklistItemKey {
    @JsonProperty("descricao_processo")
    DESCRICAO_PROCESSO,
    @JsonProperty("objetivo_escopo")
    OBJETIVO_ESCOPO,
    @JsonProperty("casos_uso")
    CASOS_USO,
    @JsonProperty("fluxos_alternativos")
    FLUXOS_ALTERNATIVOS,
    @JsonProperty("regras_negocio")
    REGRAS_NEGOCIO,
    @JsonProperty("tratamento_excecoes")
    TRATAMENTO_EXCECOES,
    @JsonProperty("inputs_outputs")
    INPUTS_OUTPUTS,
    @JsonProperty("campos_estrutura_dados")
    CAMPOS_ESTRUTURA_DADOS,
    @JsonProperty("dependencias")
    DEPENDENCIAS,
    @JsonProperty("controle_acesso")
    CONTROLE_ACESSO,
    @JsonProperty("volume_frequencia")
    VOLUME_FREQUENCIA,
    @JsonProperty("logs_reprocessamento")
    LOGS_REPROCESSAMENTO,
    @JsonProperty("mensagens_validacoes")
    MENSAGENS_VALIDACOES,
    @JsonProperty("condicoes_teste")
    CONDICOES_TESTE,
    @JsonProperty("massa_dados")
    MASSA_DADOS,
    @JsonEnumDefaultValue
    UNKNOWN
}
