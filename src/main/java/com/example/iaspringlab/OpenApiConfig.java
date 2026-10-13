package com.example.iaspringlab;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("AI Spring Lab - API de gestion de commandes")
                .description("Clients, produits et commandes (statuts DRAFT/CONFIRMED/CANCELLED).")
                .version("v1"));
    }
}
