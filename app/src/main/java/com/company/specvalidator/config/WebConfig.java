package com.company.specvalidator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Futuramente, restringir origens por ambiente via application.yml (ex: app.cors.allowed-origins)
    // Futuramente, integrar Spring Security com Keycloak para autenticacao/autorizacao por usuario/tenant

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedOrigins("http://localhost:3000", "http://localhost:4200", "http://localhost:5173")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
