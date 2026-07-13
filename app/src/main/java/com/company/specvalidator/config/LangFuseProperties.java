package com.company.specvalidator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "langfuse")
public class LangFuseProperties {
    private boolean enabled = false;
    private String host = "https://cloud.langfuse.com";
    private String publicKey;
    private String secretKey;
}
