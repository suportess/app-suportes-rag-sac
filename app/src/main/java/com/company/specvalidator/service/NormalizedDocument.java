package com.company.specvalidator.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedDocument {
    private String rawText;
    private String normalizedText;
    private Map<String, String> sections;
}
