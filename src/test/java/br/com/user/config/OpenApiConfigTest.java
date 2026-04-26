package br.com.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void openAPIShouldHaveCorrectTitle() {
        OpenAPI openAPI = openApiConfig.openAPI();
        assertEquals("API de Usuários", openAPI.getInfo().getTitle());
    }

    @Test
    void openAPIShouldHaveCorrectVersion() {
        OpenAPI openAPI = openApiConfig.openAPI();
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
    }

    @Test
    void openAPIShouldHaveLocalServer() {
        OpenAPI openAPI = openApiConfig.openAPI();
        assertFalse(openAPI.getServers().isEmpty());
        assertEquals("http://localhost:8082", openAPI.getServers().get(0).getUrl());
    }

    @Test
    void openAPIShouldHaveBearerAuthScheme() {
        OpenAPI openAPI = openApiConfig.openAPI();
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("bearerAuth"));
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
    }

    @Test
    void openAPIShouldHaveGlobalSecurityRequirement() {
        OpenAPI openAPI = openApiConfig.openAPI();
        assertFalse(openAPI.getSecurity().isEmpty());
        assertTrue(openAPI.getSecurity().get(0).containsKey("bearerAuth"));
    }
}
