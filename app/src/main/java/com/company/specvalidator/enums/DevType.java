package com.company.specvalidator.enums;

public enum DevType {
    REPORT("Report/Relatorio ALV"),
    ENHANCEMENT("Enhancement/Exit/BADI"),
    INTERFACE("Interface/PI-CPI"),
    WORKFLOW("Workflow/Fluxo de Aprovacao"),
    FORMS("Formulario (SmartForm/SapScript/Adobe Forms)"),
    BATCH("Conversao/Batch Input/BDC"),
    TABLE("Tabela ou Estrutura ABAP Customizada"),
    ARQUIVO("Arquivo (Importacao/Exportacao de Arquivo Plano)"),
    TELA_FIORI("Tela Customizada/Aplicativo Fiori"),
    UNKNOWN("Tipo nao identificado - aplicando criterios gerais");

    private final String displayName;

    DevType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
