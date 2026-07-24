package com.company.specvalidator.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum ValidationStatus {
    APROVADO,
    ACEITAVEL,
    @JsonEnumDefaultValue
    REPROVADO
}
