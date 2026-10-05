package br.edu.pucgoias.linkhealth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.SecurityRequirement;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI linkHealthOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        "ApiKey",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")))
                .addSecurityItem(new SecurityRequirement().addList("ApiKey"))
                .info(new Info()
                        .title("Link Health API")
                        .version("v1")
                        .description("API REST para integração entre o aplicativo Android e os recursos do Link Health.")
                        .license(new License().name("Uso acadêmico - Projeto Integrador")));
    }
}
