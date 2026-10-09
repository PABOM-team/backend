package com.pabom.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    public static final String SIGNUP_BEARER_AUTH = "signupBearerAuth";

    @Bean
    public OpenAPI pabomOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        SIGNUP_BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("소셜 로그인 응답의 signupToken 원문을 입력하세요.")
                ))
                .info(new Info()
                        .title("Pabom API")
                        .description("Pabom Backend API Documentation")
                        .version("v1.0.0"));
    }
}
