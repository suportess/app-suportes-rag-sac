package com.company.specvalidator.service.ai;

import java.util.Map;

/**
 * Item de um Dataset da Langfuse (Prompt/Dataset Management), buscado via API publica.
 */
public record DatasetItemDto(String id, String input, Map<String, Object> expectedOutput, Map<String, Object> metadata) {
}
