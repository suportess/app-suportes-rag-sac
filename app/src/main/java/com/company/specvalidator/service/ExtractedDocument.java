package com.company.specvalidator.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedDocument {
    private String rawText;
    private Integer pageCount;
    @Builder.Default
    private java.util.Map<String, String> sections = new java.util.LinkedHashMap<>();
}
