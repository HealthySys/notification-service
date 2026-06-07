package br.unifor.healthsys.notification.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "HealthSys - Notification Service API",
                version = "1.0.0",
                description = """
                        Serviço de notificações da plataforma HealthSys.

                        Responsabilidades:
                        - Consulta da timeline de notificações por perfil (`/api/notifications`)
                        - Broadcast de eventos clínicos
                        - Canal em tempo real via WebSocket/STOMP (`/ws`)

                        Requer token JWT emitido pelo user-service.""",
                contact = @Contact(name = "HealthSys - UNIFOR", email = "healthsys@unifor.br"),
                license = @License(name = "Uso acadêmico/interno")
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtido em POST /api/auth/login (user-service). Informe apenas o token (sem o prefixo 'Bearer')."
)
public class OpenApiConfig {
}
