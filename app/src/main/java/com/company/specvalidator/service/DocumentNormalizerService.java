package com.company.specvalidator.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DocumentNormalizerService {

    public NormalizedDocument normalize(String rawText) {
        String noDupSpaces = rawText.replaceAll("[ \\t]{2,}", " ");
        String noExcessLines = noDupSpaces.replaceAll("\\n{3,}", "\\n\\n");
        String normalized = removeRepeatingHeaders(noExcessLines).trim();

        return NormalizedDocument.builder()
                .rawText(rawText)
                .normalizedText(normalized)
                .build();
    }

    private String removeRepeatingHeaders(String text) {
        String[] lines = text.split("\\R");
        Map<String, Integer> counters = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String normalizedLine = line.trim().toLowerCase();
            if (!normalizedLine.isBlank()) {
                counters.put(normalizedLine, counters.getOrDefault(normalizedLine, 0) + 1);
            }
        }

        for (String line : lines) {
            String normalizedLine = line.trim().toLowerCase();
            Integer occurrences = counters.getOrDefault(normalizedLine, 0);
            if (occurrences > 8 && normalizedLine.length() < 40) {
                continue;
            }
            sb.append(line).append("\n");
        }

        return sb.toString();
    }
}
