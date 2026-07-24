package com.company.specvalidator.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum ChecklistStatus {
    OK,
    PARCIAL,
    AUSENTE;

    // A IA pode retornar "Parcial"/"Ausente" (grafia usada no proprio texto do prompt) em vez
    // de "PARCIAL"/"AUSENTE". Sem normalizar aqui, o mismatch de case cairia silenciosamente
    // em AUSENTE via enum default, tratando um item Parcial como se estivesse Ausente.
    @JsonCreator
    public static ChecklistStatus fromJson(String value) {
        if (value == null) {
            return AUSENTE;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "OK" -> OK;
            case "PARCIAL" -> PARCIAL;
            default -> AUSENTE;
        };
    }
}
