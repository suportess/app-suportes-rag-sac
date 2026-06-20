package com.company.specvalidator.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum IssueSeverity {
    CRITICAL,
    MODERATE,
    @JsonEnumDefaultValue
    MINOR
}
