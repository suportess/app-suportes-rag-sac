package com.company.specvalidator.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentNormalizerService {

    private static final List<String> EXPECTED_SECTIONS = List.of(
            "Objetivo", "Escopo", "Fora de escopo", "Regras de negocio", "Requisitos funcionais",
            "Requisitos nao funcionais", "Processo atual", "Processo futuro", "Integracoes", "Tabelas",
            "Campos", "Transacoes", "Mensagens", "Criterios de aceite", "Cenarios de teste", "Impactos"
    );

    public NormalizedDocument normalize(String rawText) {
        String noDupSpaces = rawText.replaceAll("[ \\t]{2,}", " ");
        String noExcessLines = noDupSpaces.replaceAll("\\n{3,}", "\\n\\n");
        String normalized = removeRepeatingHeaders(noExcessLines).trim();

        Map<String, String> sections = detectSections(normalized);

        return NormalizedDocument.builder()
                .rawText(rawText)
                .normalizedText(normalized)
                .sections(sections)
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

    private Map<String, String> detectSections(String text) {
        Map<String, Integer> starts = new LinkedHashMap<>();
        for (String section : EXPECTED_SECTIONS) {
            Pattern pattern = Pattern.compile("(?im)^\\s*" + Pattern.quote(section) + "\\s*[:\\-]?\\s*$");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                starts.put(section, matcher.start());
            }
        }

        Map<String, String> sections = new LinkedHashMap<>();
        List<Map.Entry<String, Integer>> ordered = starts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .toList();

        for (int i = 0; i < ordered.size(); i++) {
            String sectionName = ordered.get(i).getKey();
            int start = ordered.get(i).getValue();
            int end = i + 1 < ordered.size() ? ordered.get(i + 1).getValue() : text.length();
            sections.put(sectionName, text.substring(start, end).trim());
        }

        return sections;
    }
}
