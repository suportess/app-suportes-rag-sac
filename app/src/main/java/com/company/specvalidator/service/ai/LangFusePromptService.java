package com.company.specvalidator.service.ai;

import com.company.specvalidator.config.LangFuseProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Busca o prompt versionado na Langfuse (Prompt Management) e compila as variaveis {{...}}.
 * Cacheia em memoria por CACHE_TTL pra nao bater na Langfuse a cada validacao. Se a Langfuse
 * estiver desabilitada, o prompt nao existir/nao tiver label "production", ou a chamada falhar
 * (rede fora, timeout), retorna Optional.empty() — quem chama deve cair no fallback hardcoded
 * do PromptBuilderService. A validacao NUNCA deve depender da Langfuse estar no ar.
 */
@Slf4j
@Component
public class LangFusePromptService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final String LABEL = "production";

    private final LangFuseProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private volatile CachedPrompt cache;

    public LangFusePromptService(LangFuseProperties properties,
                                 RestTemplateBuilder restTemplateBuilder,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Optional<PromptResult> buildPrompt(Map<String, String> variables) {
        if (!properties.isEnabled() || properties.getPromptName() == null || properties.getPromptName().isBlank()) {
            return Optional.empty();
        }

        FetchedPrompt fetched = fetchCached();
        if (fetched == null) {
            return Optional.empty();
        }

        String system = compile(fetched.systemTemplate(), variables);
        String user = compile(fetched.userTemplate(), variables);
        return Optional.of(new PromptResult(system, user, fetched.version()));
    }

    private FetchedPrompt fetchCached() {
        CachedPrompt current = cache;
        if (current != null && current.fetchedAt().plus(CACHE_TTL).isAfter(Instant.now())) {
            return current.prompt();
        }
        try {
            FetchedPrompt fetched = fetchFromLangfuse();
            cache = new CachedPrompt(fetched, Instant.now());
            return fetched;
        } catch (Exception e) {
            log.warn("LangFuse: falha ao buscar prompt '{}' (label={}) - {}. Usando fallback.",
                    properties.getPromptName(), LABEL, e.getMessage());
            // Mantem servindo a ultima versao valida em cache, mesmo vencida, em vez de cair
            // pro fallback hardcoded por uma falha transitoria de rede.
            return current != null ? current.prompt() : null;
        }
    }

    private FetchedPrompt fetchFromLangfuse() throws Exception {
        String url = UriComponentsBuilder
                .fromHttpUrl(properties.getHost() + "/api/public/v2/prompts/" + properties.getPromptName())
                .queryParam("label", LABEL)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(properties.getPublicKey(), properties.getSecretKey());

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode promptNode = root.path("prompt");
        if (!promptNode.isArray()) {
            throw new IllegalStateException("Prompt '" + properties.getPromptName() + "' nao e do tipo chat (system+user)");
        }

        String systemTemplate = null;
        String userTemplate = null;
        for (JsonNode message : promptNode) {
            String role = message.path("role").asText();
            String content = message.path("content").asText();
            if ("system".equals(role)) systemTemplate = content;
            if ("user".equals(role)) userTemplate = content;
        }
        if (systemTemplate == null || systemTemplate.isBlank() || userTemplate == null || userTemplate.isBlank()) {
            throw new IllegalStateException("Prompt '" + properties.getPromptName() + "' sem mensagens system/user validas");
        }

        return new FetchedPrompt(systemTemplate, userTemplate, root.path("version").asInt());
    }

    private String compile(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private record FetchedPrompt(String systemTemplate, String userTemplate, Integer version) {
    }

    private record CachedPrompt(FetchedPrompt prompt, Instant fetchedAt) {
    }
}
