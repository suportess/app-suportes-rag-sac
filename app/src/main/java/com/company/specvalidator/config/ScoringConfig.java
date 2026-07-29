package com.company.specvalidator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ScoringConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.scoring")
    public ScoringProperties scoringProperties() {
        return new ScoringProperties();
    }

    @Data
    public static class ScoringProperties {
        // Percentual do peso conquistado quando o item vem "Parcial" (OK = 100%, Ausente = 0%
        // sempre; so o Parcial e configuravel, definido pelo negocio como ponto de partida).
        private double parcialMultiplier = 0.5;

        // Peso (1-5, definido pelo negocio) de cada criterio, chaveado pela mesma string usada
        // no JSON da IA (ex: "regras_negocio"). Nao inclui "consistencia" — esse criterio saiu
        // do checklist pontuado e virou regra transversal no prompt.
        private Map<String, Integer> pesos = new LinkedHashMap<>();
    }
}
