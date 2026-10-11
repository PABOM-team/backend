package com.pabom.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    public static final String AUTHORIZATION = "Authorization";
    private static final List<String> AUTH_PATH_ORDER = List.of(
            "/api/v1/auth/{provider}/login-url",
            "/api/v1/auth/{provider}/callback",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
    );

    @Bean
    public OpenAPI pabomOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        AUTHORIZATION,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Bearer 접두사 없이 JWT 토큰 원문을 입력하세요.")
                ))
                .info(new Info()
                        .title("Pabom API")
                        .description("Pabom Backend API Documentation")
                        .version("v1.0.0"));
    }

    @Bean
    public OpenApiCustomizer authOperationOrderCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            Paths originalPaths = openApi.getPaths();
            Paths orderedPaths = new Paths();
            AUTH_PATH_ORDER.forEach(path -> {
                if (originalPaths.containsKey(path)) {
                    orderedPaths.addPathItem(path, originalPaths.get(path));
                }
            });
            originalPaths.forEach(orderedPaths::putIfAbsent);
            openApi.setPaths(orderedPaths);
        };
    }
}
