package com.atlante.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentazione interattiva su /swagger-ui.html. Il pulsante "Authorize" accetta il JWT
 * ottenuto da /api/auth/login, così si possono provare anche gli endpoint protetti.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearer-jwt";

    @Bean
    public OpenAPI atlanteOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Atlante delle Influenze — API")
                        .version("1.0")
                        .description("""
                                Connessioni critiche tra cinema e arte visiva: opere, connessioni, \
                                like, commenti, preferiti, chat (STOMP su /ws), notifiche, AI e amministrazione."""))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
