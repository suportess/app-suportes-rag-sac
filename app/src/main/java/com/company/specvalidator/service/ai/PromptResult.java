package com.company.specvalidator.service.ai;

/**
 * Resultado de um prompt pronto pra enviar ao provider de IA. promptVersion vem preenchido
 * quando o prompt foi buscado da Langfuse (Prompt Management) e fica null quando o fallback
 * hardcoded do PromptBuilderService foi usado (Langfuse desabilitada, prompt nao encontrado
 * ou erro de rede) — usado pra linkar a generation a versao exata do prompt na Langfuse.
 */
public record PromptResult(String systemPrompt, String userPrompt, Integer promptVersion) {
}
