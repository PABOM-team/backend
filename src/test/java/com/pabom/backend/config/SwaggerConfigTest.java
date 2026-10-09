package com.pabom.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class SwaggerConfigTest {

    @Test
    void ordersOauthBeforeRefreshAndLogout() {
        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/v1/auth/logout", new PathItem())
                .addPathItem("/api/v1/auth/refresh", new PathItem())
                .addPathItem("/api/v1/auth/{provider}/callback", new PathItem())
                .addPathItem("/api/v1/auth/{provider}/login-url", new PathItem())
                .addPathItem("/api/v1/health", new PathItem()));

        new SwaggerConfig().authOperationOrderCustomizer().customise(openApi);

        assertThat(new ArrayList<>(openApi.getPaths().keySet())).containsExactly(
                "/api/v1/auth/{provider}/login-url",
                "/api/v1/auth/{provider}/callback",
                "/api/v1/auth/refresh",
                "/api/v1/auth/logout",
                "/api/v1/health"
        );
    }
}
