package com.company.specvalidator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
public class OpenAiConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.ai")
    public AiProperties aiProperties() {
        return new AiProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.storage")
    public StorageProperties storageProperties() {
        return new StorageProperties();
    }

    @Data
    @Validated
    public static class AiProperties {
        private String provider;
        private OpenAi openai = new OpenAi();

        @Data
        public static class OpenAi {
            private String apiKey;
            private String model = "gpt-4.1-mini";
            private double temperature = 0.1;
            private int timeoutSeconds = 120;
            private int maxRetries = 3;
        }
    }

    @Data
    @Validated
    public static class StorageProperties {
        private String basePath = "./uploads";
    }
}
