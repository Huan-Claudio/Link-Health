package br.edu.pucgoias.linkhealth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI linkHealthOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Link Health API")
                .version("v1")
                .description("API REST para integração entre o aplicativo Android e os recursos do Link Health.")
                .license(new License().name("Uso acadêmico - Projeto Integrador")));
    }
}
