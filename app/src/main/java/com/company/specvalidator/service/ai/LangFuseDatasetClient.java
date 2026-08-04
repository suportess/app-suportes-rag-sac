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
