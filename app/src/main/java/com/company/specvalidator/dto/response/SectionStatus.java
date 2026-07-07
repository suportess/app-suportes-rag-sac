package com.company.specvalidator.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionStatus {
    private String sectionName;
    private String status; // PRESENTE, PARCIAL, AUSENTE
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String detectedHeading;
}
