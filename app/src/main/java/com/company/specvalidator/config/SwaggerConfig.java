package com.company.specvalidator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SAP ABAP Spec Validator API")
                        .version("v1")
                        .description("API para validar especificacoes funcionais SAP ABAP com apoio de IA. "
                                + "Realiza upload de documentos PDF/DOCX, extrai texto, normaliza conteudo, "
                                + "executa validacoes locais e envia para IA gerar relatorio estruturado.")
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("SAP Spec Validator Team")));
    }
}
