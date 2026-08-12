package com.company.specvalidator.service.ai;

import com.company.specvalidator.config.LangFuseProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Busca itens de Datasets da Langfuse e registra a execucao de cada item (Dataset Run Item) —
 * usado pra rodar o pipeline real de validacao contra um dataset de referencia e comparar os
 * resultados na aba Experiments da Langfuse. Falhas aqui nunca devem derrubar a validacao
 * principal do app — sao sempre logadas como warning e engolidas.
 */
@Slf4j
@Component
public class LangFuseDatasetClient {

    private final LangFuseProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LangFuseDatasetClient(LangFuseProperties properties,
                                 RestTemplateBuilder restTemplateBuilder,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<DatasetItemDto> fetchItems(String datasetName) {
        if (!properties.isEnabled()) return List.of();
        List<DatasetItemDto> items = new ArrayList<>();
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(properties.getHost() + "/api/public/dataset-items")
                    .queryParam("datasetName", datasetName)
                    .queryParam("limit", 100)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            for (JsonNode node : root.path("data")) {
                items.add(new DatasetItemDto(
                        node.path("id").asText(),
                        node.path("input").asText(),
                        toMap(node.path("expectedOutput")),
                        toMap(node.path("metadata"))
                ));
            }
        } catch (Exception e) {
            log.warn("LangFuse: falha ao buscar itens do dataset '{}' - {}", datasetName, e.getMessage());
        }
        return items;
    }

    public void linkRunItem(String runName, String datasetItemId, String traceId) {
        if (!properties.isEnabled()) return;
        try {
            HttpHeaders headers = authHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "runName", runName,
                    "datasetItemId", datasetItemId,
                    "traceId", traceId
            );

            restTemplate.exchange(
                    properties.getHost() + "/api/public/dataset-run-items",
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                    String.class
            );
        } catch (Exception e) {
            log.warn("LangFuse: falha ao linkar item '{}' na run '{}' - {}", datasetItemId, runName, e.getMessage());
        }
    }

    public Optional<String> fetchDatasetId(String datasetName) {
        if (!properties.isEnabled()) return Optional.empty();
        try {
            String url = properties.getHost() + "/api/public/datasets/" + datasetName;
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String id = root.path("id").asText(null);
            return Optional.ofNullable(id);
        } catch (Exception e) {
            log.warn("LangFuse: falha ao buscar id do dataset '{}' - {}", datasetName, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Busca score/classificacao final de cada item de um run ja executado, lendo o output da
     * trace linkada. Usado pra comparar um run atual contra um run baseline (regressao entre
     * versoes de prompt) — nao reprocessa nada, so le o que ja foi gravado na Langfuse.
     */
    public List<RunItemScore> fetchRunResults(String datasetId, String runName) {
        if (!properties.isEnabled()) return List.of();
        List<RunItemScore> results = new ArrayList<>();
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(properties.getHost() + "/api/public/dataset-run-items")
                    .queryParam("datasetId", datasetId)
                    .queryParam("runName", runName)
                    .queryParam("limit", 100)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            for (JsonNode item : root.path("data")) {
                String datasetItemId = item.path("datasetItemId").asText();
                String traceId = item.path("traceId").asText();
                RunItemScore scoreEntry = fetchTraceScore(traceId, datasetItemId);
                results.add(scoreEntry);
                try {
                    Thread.sleep(400);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            log.warn("LangFuse: falha ao buscar resultados do run '{}' - {}", runName, e.getMessage());
        }
        return results;
    }

    private RunItemScore fetchTraceScore(String traceId, String datasetItemId) {
        try {
            String url = properties.getHost() + "/api/public/traces/" + traceId;
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode output = root.path("output");
            Integer score = output.hasNonNull("score") ? output.path("score").asInt() : null;
            String classificacao = output.hasNonNull("classificacao") ? output.path("classificacao").asText() : null;
            return new RunItemScore(datasetItemId, traceId, score, classificacao);
        } catch (Exception e) {
            log.warn("LangFuse: falha ao buscar output da trace '{}' - {}", traceId, e.getMessage());
            return new RunItemScore(datasetItemId, traceId, null, null);
        }
    }

    /**
     * Busca a versao de prompt usada numa trace, lendo a observation do tipo GENERATION.
     * Como todos os itens de um mesmo run usam a mesma versao de prompt, uma trace representativa
     * ja e suficiente — nao precisa buscar item por item.
     */
    public Optional<Integer> fetchPromptVersion(String traceId) {
        if (!properties.isEnabled() || traceId == null) return Optional.empty();
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(properties.getHost() + "/api/public/observations")
                    .queryParam("traceId", traceId)
                    .queryParam("limit", 20)
                    .toUriString();
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            for (JsonNode obs : root.path("data")) {
                if (obs.hasNonNull("promptVersion")) {
                    return Optional.of(obs.path("promptVersion").asInt());
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("LangFuse: falha ao buscar promptVersion da trace '{}' - {}", traceId, e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return Map.of();
        return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
        });
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(properties.getPublicKey(), properties.getSecretKey());
        return headers;
    }
}
