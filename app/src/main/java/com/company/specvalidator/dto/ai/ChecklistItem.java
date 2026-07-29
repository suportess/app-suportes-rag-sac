package com.company.specvalidator.dto.ai;

import com.company.specvalidator.enums.ChecklistItemKey;
import com.company.specvalidator.enums.ChecklistStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItem {
    private ChecklistItemKey chave;
    private String item;
    private ChecklistStatus status;
    private String comentario;
    private Integer pontos;
    private Integer peso;
    private Double pontosConquistados;
    private boolean aplicavel;
}
