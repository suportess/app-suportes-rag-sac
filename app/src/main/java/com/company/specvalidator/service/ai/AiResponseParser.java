package com.company.specvalidator.service.ai;

import com.company.specvalidator.dto.ai.AiValidationResponse;
import com.company.specvalidator.exception.AiProviderException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AiResponseParser {

    private static final Pattern REASONING_CLOSING_MARKER = Pattern.compile("(?m)^\\s*\\]\\s*$");

    private final ObjectMapper objectMapper;

    public AiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public AiValidationResponse parse(String rawResponse) {
        try {
            String normalized = cleanup(rawResponse);
            return objectMapper.readValue(normalized, AiValidationResponse.class);
        } catch (Exception e) {
            throw new AiProviderException("IA retornou JSON invalido", e);
        }
    }

    private String cleanup(String rawResponse) {
        String output = rawResponse == null ? "" : rawResponse.trim();
        if (output.startsWith("```json")) {
            output = output.substring(7);
        }
        if (output.startsWith("```")) {
            output = output.substring(3);
        }
        if (output.endsWith("```")) {
            output = output.substring(0, output.length() - 3);
        }
        output = output.trim();

        output = stripReasoningBlock(output);

        return output.trim();
    }

    private String stripReasoningBlock(String output) {
        int reasoningStart = output.indexOf("RACIOCINIO [");
        if (reasoningStart == -1) {
            return output;
        }
        Matcher closingMarker = REASONING_CLOSING_MARKER.matcher(output);
        if (closingMarker.find(reasoningStart)) {
            return output.substring(closingMarker.end());
        }
        return output;
    }
}
