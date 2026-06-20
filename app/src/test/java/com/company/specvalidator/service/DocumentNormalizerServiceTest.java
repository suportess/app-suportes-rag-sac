package com.company.specvalidator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentNormalizerServiceTest {

    private DocumentNormalizerService service;

    @BeforeEach
    void setUp() {
        service = new DocumentNormalizerService();
    }

    @Test
    void testNormalizeRemovesDuplicateSpaces() {
        String input = "Texto  com   muitos    espacos";
        NormalizedDocument result = service.normalize(input);
        assertFalse(result.getNormalizedText().contains("  "),
                "Normalized text should not contain consecutive spaces");
        assertTrue(result.getNormalizedText().contains("Texto com muitos espacos"));
    }

    @Test
    void testNormalizeRemovesExcessNewlines() {
        String input = "Linha 1\n\n\n\nLinha 2\n\n\n\n\nLinha 3";
        NormalizedDocument result = service.normalize(input);
        String normalized = result.getNormalizedText();
        assertFalse(normalized.contains("\n\n\n"),
                "Normalized text should not contain 3+ consecutive newlines");
        assertTrue(normalized.contains("Linha 1"));
        assertTrue(normalized.contains("Linha 2"));
        assertTrue(normalized.contains("Linha 3"));
    }

    @Test
    void testNormalizeRemovesRepeatingHeaders() {
        StringBuilder sb = new StringBuilder();
        // A short line repeated more than 8 times should be removed
        for (int i = 0; i < 10; i++) {
            sb.append("Header Repetido\n");
        }
        sb.append("Conteudo real do documento");

        NormalizedDocument result = service.normalize(sb.toString());
        assertFalse(result.getNormalizedText().contains("Header Repetido"),
                "Repeating headers (>8 occurrences, <40 chars) should be removed");
        assertTrue(result.getNormalizedText().contains("Conteudo real do documento"));
    }

    @Test
    void testRepeatingHeaderNotRemovedWhenBelowThreshold() {
        StringBuilder sb = new StringBuilder();
        // A short line repeated exactly 8 times should NOT be removed (threshold is >8)
        for (int i = 0; i < 8; i++) {
            sb.append("Header OK\n");
        }
        sb.append("Conteudo do documento");

        NormalizedDocument result = service.normalize(sb.toString());
        assertTrue(result.getNormalizedText().contains("Header OK"),
                "Lines repeated exactly 8 times should not be removed");
    }

    @Test
    void testDetectsSections() {
        String input = "Objetivo\nDescrever a solucao para o cliente.\n\n" +
                "Escopo\nImplementacao do modulo MM.\n\n" +
                "Criterios de aceite\nTodos os testes devem passar.";

        NormalizedDocument result = service.normalize(input);
        assertNotNull(result.getSections());
        assertTrue(result.getSections().containsKey("Objetivo"),
                "Should detect 'Objetivo' section");
        assertTrue(result.getSections().containsKey("Escopo"),
                "Should detect 'Escopo' section");
        assertTrue(result.getSections().containsKey("Criterios de aceite"),
                "Should detect 'Criterios de aceite' section");
    }

    @Test
    void testHandlesNullInput() {
        // The normalize method does not check for null, so it should throw NPE
        assertThrows(NullPointerException.class, () -> service.normalize(null));
    }

    @Test
    void testEmptyText() {
        NormalizedDocument result = service.normalize("");
        assertNotNull(result);
        assertEquals("", result.getRawText());
        assertTrue(result.getNormalizedText().isEmpty());
        assertNotNull(result.getSections());
        assertTrue(result.getSections().isEmpty());
    }

    @Test
    void testRawTextIsPreserved() {
        String input = "Texto  original   com   espacos";
        NormalizedDocument result = service.normalize(input);
        assertEquals(input, result.getRawText(),
                "rawText should preserve the original unmodified text");
    }
}
