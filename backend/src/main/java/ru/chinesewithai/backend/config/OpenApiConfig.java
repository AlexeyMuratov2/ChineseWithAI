package ru.chinesewithai.backend.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH_SCHEME,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter JWT access token without the Bearer prefix. Swagger UI adds it automatically.")
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";
}
