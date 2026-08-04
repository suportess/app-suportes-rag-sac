package com.company.specvalidator.service.ai;

import com.company.specvalidator.config.LangFuseProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class LangFuseClient {

    private static final String INGESTION_PATH = "/api/public/ingestion";

    private final LangFuseProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LangFuseClient(LangFuseProperties properties,
                          RestTemplateBuilder restTemplateBuilder,
                          ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void startTrace(String traceId, String name, Map<String, Object> metadata) {
        startTrace(traceId, name, null, null, metadata);
    }

    /**
     * sessionId agrupa varias traces na aba Sessions da Langfuse — usado pra rodar o mesmo
     * documento/dataset varias vezes e comparar as execucoes juntas (testes de consistencia,
     * dataset runs).
     */
    public void startTrace(String traceId, String name, String sessionId, Map<String, Object> metadata) {
        startTrace(traceId, name, sessionId, null, metadata);
    }

    /**
     * input fica gravado como o campo "input" nativo da trace — e' o que a aba Datasets >
     * Experiments usa pra montar a coluna "Input" comparando com o expected_output do item.
     * Sem isso a coluna fica vazia mesmo com tudo funcionando (so o "output", setado no
     * endTrace, aparece).
     */
    public void startTrace(String traceId, String name, String sessionId, Object input, Map<String, Object> metadata) {
        if (!properties.isEnabled()) return;
        try {
            Map<String, Object> traceBody = new HashMap<>();
            traceBody.put("id", traceId);
            traceBody.put("name", name);
            traceBody.put("environment", properties.getEnvironment());
            if (sessionId != null) traceBody.put("sessionId", sessionId);
            if (input != null) traceBody.put("input", input);
            if (metadata != null) traceBody.put("metadata", metadata);

            ingest(List.of(event("trace-create", traceBody)));
        } catch (Exception e) {
            log.warn("LangFuse: erro ao iniciar trace {} - {}", name, e.getMessage());
        }
    }

    /**
     * Finaliza o trace raiz com o resultado final da validacao. A Langfuse trata um novo
     * evento trace-create com o mesmo id como upsert (so os campos enviados aqui sao
     * sobrescritos, o resto do trace ja criado em startTrace e preservado).
     */
    public void endTrace(String traceId, Object output, List<String> tags) {
        if (!properties.isEnabled()) return;
        try {
            Map<String, Object> traceBody = new HashMap<>();
            traceBody.put("id", traceId);
            if (output != null) traceBody.put("output", output);
            if (tags != null && !tags.isEmpty()) traceBody.put("tags", tags);

            ingest(List.of(event("trace-create", traceBody)));
        } catch (Exception e) {
            log.warn("LangFuse: erro ao finalizar trace {} - {}", traceId, e.getMessage());
        }
    }

    public String startSpan(String traceId, String parentSpanId, String name, Object input) {
        String spanId = UUID.randomUUID().toString();
        if (!properties.isEnabled()) return spanId;
        try {
            Map<String, Object> spanBody = new HashMap<>();
            spanBody.put("id", spanId);
            spanBody.put("traceId", traceId);
            if (parentSpanId != null) spanBody.put("parentObservationId", parentSpanId);
            spanBody.put("name", name);
            spanBody.put("startTime", Instant.now().toString());
            spanBody.put("environment", properties.getEnvironment());
            if (input != null) spanBody.put("input", input);

            ingest(List.of(event("span-create", spanBody)));
        } catch (Exception e) {
            log.warn("LangFuse: erro ao iniciar span {} - {}", name, e.getMessage());
        }
        return spanId;
    }

    public void endSpan(String spanId, Object output, Map<String, Object> metadata) {
        if (!properties.isEnabled()) return;
        try {
            Map<String, Object> spanBody = new HashMap<>();
            spanBody.put("id", spanId);
            spanBody.put("endTime", Instant.now().toString());
            if (output != null) spanBody.put("output", output);
            if (metadata != null) spanBody.put("metadata", metadata);

            ingest(List.of(event("span-update", spanBody)));
        } catch (Exception e) {
            log.warn("LangFuse: erro ao finalizar span - {}", e.getMessage());
        }
    }

    public void endSpanWithError(String spanId, String errorMessage) {
        if (!properties.isEnabled()) return;
        try {
            Map<String, Object> spanBody = new HashMap<>();
            spanBody.put("id", spanId);
            spanBody.put("endTime", Instant.now().toString());
            spanBody.put("level", "ERROR");
            spanBody.put("statusMessage", errorMessage);

            ingest(List.of(event("span-update", spanBody)));
        } catch (Exception e) {
            log.warn("LangFuse: erro ao finalizar span com erro - {}", e.getMessage());
        }
    }

    public void recordGenerationStart(String traceId, String generationId,
                                      List<Map<String, Object>> messages,
                                      String model, double temperature,
                                      Instant startTime, Integer promptVersion) {
        if (!properties.isEnabled()) return;
        try {
            Map<String, Object> genBody = new HashMap<>();
            genBody.put("id", generationId);
            genBody.put("traceId", traceId);
            genBody.put("name", "openai-completion");
            genBody.put("model", model);
            genBody.put("modelParameters", Map.of("temperature", temperature));
            genBody.put("input", messages);
            genBody.put("startTime", startTime.toString());
            genBody.put("environment", properties.getEnvironment());
            if (promptVersion != null) {
                genBody.put("promptName", properties.getPromptName());
                genBody.put("promptVersion", promptVersion);
            }

            ingest(List.of(event("generation-create", genBody)));
        } catch (Exception e) {
            log.warn("LangFuse: erro ao registrar inicio da geracao - {}", e.getMessage());
        }
    }

    public void recordGenerationSuccess(String generationId, String output,
                                        int inputTokens, int outputTokens,
                                        Instant endTime) {
        if (!properties.isEnabled()) return;
        try {
            Map<String, Object> genBody = new HashMap<>();
            genBody.put("id", generationId);
            genBody.put("output", output);
            genBody.put("endTime", endTime.toString());
            genBody.put("usage", Map.of("input", inputTokens, "output", outputTokens));

            ingest(List.of(event("generation-update", genBody)));
        } catch (Exception e) {
            log.warn("LangFuse: erro ao registrar sucesso da geracao - {}", e.getMessage());
        }
    }

    public void recordGenerationError(String generationId, String errorMessage, Instant endTime) {
        if (!properties.isEnabled()) return;
        try {
            Map<String, Object> genBody = new HashMap<>();
            genBody.put("id", generationId);
            genBody.put("level", "ERROR");
            genBody.put("statusMessage", errorMessage);
            genBody.put("endTime", endTime.toString());

            ingest(List.of(event("generation-update", genBody)));
        } catch (Exception e) {
            log.warn("LangFuse: erro ao registrar falha da geracao - {}", e.getMessage());
        }
    }

    private void ingest(List<Map<String, Object>> events) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, basicAuth());

        String body = objectMapper.writeValueAsString(Map.of("batch", events));

        restTemplate.exchange(
            properties.getHost() + INGESTION_PATH,
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            String.class
        );
    }

    private Map<String, Object> event(String type, Map<String, Object> body) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", UUID.randomUUID().toString());
        event.put("type", type);
        event.put("timestamp", Instant.now().toString());
        event.put("body", body);
        return event;
    }

    private String basicAuth() {
        String credentials = properties.getPublicKey() + ":" + properties.getSecretKey();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
