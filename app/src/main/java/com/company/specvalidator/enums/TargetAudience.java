package com.company.specvalidator.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum TargetAudience {
    FUNCIONAL,
    ABAP,
    ARQUITETURA,
    NEGOCIO,
    AUTORIZACAO,
    INTEGRACAO,
    DADOS,
    TESTES,
    @JsonEnumDefaultValue
    OUTROS
}
