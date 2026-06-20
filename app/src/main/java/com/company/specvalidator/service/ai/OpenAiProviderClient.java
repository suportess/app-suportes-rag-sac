package com.company.specvalidator.service.ai;

import com.company.specvalidator.config.OpenAiConfig;
import com.company.specvalidator.dto.ai.AiValidationRequest;
import com.company.specvalidator.dto.ai.AiValidationResponse;
import com.company.specvalidator.exception.AiProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiProviderClient implements AiProviderClient {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    private final OpenAiConfig.AiProperties aiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiResponseParser aiResponseParser;

    public OpenAiProviderClient(OpenAiConfig.AiProperties aiProperties,
                                RestTemplateBuilder restTemplateBuilder,
                                ObjectMapper objectMapper,
                                AiResponseParser aiResponseParser) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.aiResponseParser = aiResponseParser;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(aiProperties.getOpenai().getTimeoutSeconds()))
                .setReadTimeout(Duration.ofSeconds(aiProperties.getOpenai().getTimeoutSeconds()))
                .build();
    }

    @Override
    public AiValidationResponse validateFunctionalSpecification(AiValidationRequest request) {
        String apiKey = aiProperties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("OPENAI_API_KEY nao configurada. Defina a variavel de ambiente OPENAI_API_KEY.");
        }

        int maxRetries = aiProperties.getOpenai().getMaxRetries();
        AiProviderException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Chamando provider IA (tentativa {}/{}), modelo: {}", attempt, maxRetries, aiProperties.getOpenai().getModel());
                return doCall(apiKey, request);
            } catch (AiProviderException e) {
                lastException = e;
                if (!isRetryable(e) || attempt == maxRetries) {
                    throw e;
                }
                long waitMs = calculateBackoff(attempt);
                log.warn("Tentativa {}/{} falhou: {}. Aguardando {}ms antes de retry.", attempt, maxRetries, e.getMessage(), waitMs);
                sleep(waitMs);
            }
        }

        throw lastException;
    }

    private AiValidationResponse doCall(String apiKey, AiValidationRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getOpenai().getModel());
        body.put("temperature", aiProperties.getOpenai().getTemperature());
        body.put("messages", List.of(
                Map.of("role", "system", "content", "Voce e um revisor tecnico SAP ABAP e deve responder apenas em JSON valido, sem markdown."),
                Map.of("role", "user", "content", request.getPrompt())
        ));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    OPENAI_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new AiProviderException("Resposta da IA veio vazia");
            }

            log.info("Resposta recebida do provider IA com sucesso");
            return aiResponseParser.parse(contentNode.asText());
        } catch (ResourceAccessException e) {
            throw new AiProviderException("Timeout ao chamar provider de IA", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new AiProviderException("Rate limit do provider de IA atingido. Tente novamente em alguns minutos.", e);
        } catch (HttpClientErrorException e) {
            throw new AiProviderException("Falha ao chamar provider de IA: " + e.getStatusCode(), e);
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Erro inesperado na integracao com IA", e);
        }
    }

    private boolean isRetryable(AiProviderException e) {
        String msg = e.getMessage();
        return msg.contains("Timeout") || msg.contains("Rate limit") || msg.contains("502") || msg.contains("503") || msg.contains("429");
    }

    private long calculateBackoff(int attempt) {
        return (long) Math.pow(2, attempt) * 1000L;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("Retry interrompido", e);
        }
    }
}
