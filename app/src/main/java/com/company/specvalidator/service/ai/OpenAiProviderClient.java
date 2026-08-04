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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class OpenAiProviderClient implements AiProviderClient {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    private final OpenAiConfig.AiProperties aiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiResponseParser aiResponseParser;
    private final LangFuseClient langFuseClient;

    public OpenAiProviderClient(OpenAiConfig.AiProperties aiProperties,
                                RestTemplateBuilder restTemplateBuilder,
                                ObjectMapper objectMapper,
                                AiResponseParser aiResponseParser,
                                LangFuseClient langFuseClient) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.aiResponseParser = aiResponseParser;
        this.langFuseClient = langFuseClient;
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
        String traceId = request.getTraceId();
        AiProviderException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Chamando provider IA (tentativa {}/{}), modelo: {}", attempt, maxRetries, aiProperties.getOpenai().getModel());
                return doCall(apiKey, request, traceId);
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

    private AiValidationResponse doCall(String apiKey, AiValidationRequest request, String traceId) {
        String model = aiProperties.getOpenai().getModel();
        double temperature = aiProperties.getOpenai().getTemperature();

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", request.getSystemPrompt()),
                Map.of("role", "user", "content", request.getUserPrompt())
        );

        String generationId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        langFuseClient.recordGenerationStart(traceId, generationId, messages, model, temperature, startTime, request.getPromptVersion());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("messages", messages);

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

            String content = contentNode.asText();
            int inputTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int outputTokens = root.path("usage").path("completion_tokens").asInt(0);

            AiValidationResponse parsed;
            try {
                parsed = aiResponseParser.parse(content);
            } catch (AiProviderException e) {
                log.error("Falha ao parsear resposta da IA (documentId={}). Resposta bruta: {}",
                        request.getDocumentId(), content);
                throw e;
            }

            langFuseClient.recordGenerationSuccess(generationId, content, inputTokens, outputTokens, Instant.now());
            log.info("Resposta recebida do provider IA com sucesso");
            return parsed;
        } catch (ResourceAccessException e) {
            langFuseClient.recordGenerationError(generationId, e.getMessage(), Instant.now());
            throw new AiProviderException("Timeout ao chamar provider de IA", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            langFuseClient.recordGenerationError(generationId, e.getMessage(), Instant.now());
            throw new AiProviderException("Rate limit do provider de IA atingido. Tente novamente em alguns minutos.", e);
        } catch (HttpClientErrorException e) {
            langFuseClient.recordGenerationError(generationId, e.getMessage(), Instant.now());
            throw new AiProviderException("Falha ao chamar provider de IA: " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            // Erros 5xx (ex: 503 Service Unavailable) sao falhas transitorias do provider —
            // precisam manter o status code na mensagem para isRetryable() reconhecer e tentar de novo.
            langFuseClient.recordGenerationError(generationId, e.getMessage(), Instant.now());
            throw new AiProviderException("Falha ao chamar provider de IA: " + e.getStatusCode(), e);
        } catch (AiProviderException e) {
            langFuseClient.recordGenerationError(generationId, e.getMessage(), Instant.now());
            throw e;
        } catch (Exception e) {
            langFuseClient.recordGenerationError(generationId, e.getMessage(), Instant.now());
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
