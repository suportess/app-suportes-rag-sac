package com.company.specvalidator.dto.response;

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
public class ChecklistItemResponse {
    private ChecklistItemKey chave;
    private String item;
    private ChecklistStatus status;
    private String comentario;
    private Integer pontos;
    private Integer peso;
    private Double pontosConquistados;
}
