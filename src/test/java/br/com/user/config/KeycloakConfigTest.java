package br.com.user.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes unitários de KeycloakConfig")
class KeycloakConfigTest {

    private KeycloakConfig keycloakConfig;

    @BeforeEach
    void setUp() {
        keycloakConfig = new KeycloakConfig();
        keycloakConfig.setUrl("http://localhost:9999");
        keycloakConfig.setRealm("development");
        keycloakConfig.setClientId("dev-client");
        keycloakConfig.setClientSecret("dev-secret");
    }

    @Test
    @DisplayName("Deve criar RestClient bean")
    void deveCriarRestClientBean() {
        // Act
        RestClient restClient = keycloakConfig.keycloakAdminRestClient();

        // Assert
        assertNotNull(restClient);
    }

    @Test
    @DisplayName("Deve retornar URL do JWK Set corretamente")
    void deveRetornarUrlDoJwkSetCorretamente() {
        // Act
        String jwkSetUri = keycloakConfig.getJwkSetUri();

        // Assert
        assertEquals("http://localhost:9999/realms/development/protocol/openid-connect/certs", jwkSetUri);
    }

    @Test
    @DisplayName("Deve retornar URL de token de admin corretamente")
    void deveRetornarUrlDeTokenDeAdminCorretamente() {
        // Act
        String adminTokenUrl = keycloakConfig.getAdminTokenUrl();

        // Assert
        assertEquals("http://localhost:9999/realms/development/protocol/openid-connect/token", adminTokenUrl);
    }

    @Test
    @DisplayName("Deve retornar URL de administração de usuário corretamente")
    void deveRetornarUrlDeAdministracaoDeUsuarioCorretamente() {
        // Arrange
        String userId = "123e4567-e89b-12d3-a456-426614174000";

        // Act
        String userAdminUrl = keycloakConfig.getUserAdminUrl(userId);

        // Assert
        assertEquals("http://localhost:9999/admin/realms/development/users/123e4567-e89b-12d3-a456-426614174000", userAdminUrl);
    }

    @Test
    @DisplayName("Deve retornar URL de reset de senha corretamente")
    void deveRetornarUrlDeResetDeSenhaCorretamente() {
        // Arrange
        String userId = "123e4567-e89b-12d3-a456-426614174000";

        // Act
        String resetPasswordUrl = keycloakConfig.getResetPasswordUrl(userId);

        // Assert
        assertEquals("http://localhost:9999/admin/realms/development/users/123e4567-e89b-12d3-a456-426614174000/reset-password", resetPasswordUrl);
    }

    @Test
    @DisplayName("Deve retornar URL de token corretamente")
    void deveRetornarUrlDeTokenCorretamente() {
        // Act
        String tokenUrl = keycloakConfig.getTokenUrl();

        // Assert
        assertEquals("http://localhost:9999/realms/development/protocol/openid-connect/token", tokenUrl);
    }

    @Test
    @DisplayName("Deve retornar body de token de admin corretamente")
    void deveRetornarBodyDeTokenDeAdminCorretamente() {
        // Act
        String adminTokenBody = keycloakConfig.getAdminTokenBody();

        // Assert
        assertEquals("grant_type=client_credentials&client_id=dev-client&client_secret=dev-secret", adminTokenBody);
    }

    @Test
    @DisplayName("Deve retornar body de password grant corretamente")
    void deveRetornarBodyDePasswordGrantCorretamente() {
        // Arrange
        String username = "usuario@teste.com";
        String password = "senha123";

        // Act
        String passwordGrantBody = keycloakConfig.getPasswordGrantBody(username, password);

        // Assert
        assertEquals("grant_type=password&client_id=dev-client&client_secret=dev-secret&username=usuario@teste.com&password=senha123", passwordGrantBody);
    }

    @Test
    @DisplayName("Deve permitir configuração de URL")
    void devePermitirConfiguracaoDeUrl() {
        // Arrange
        String novaUrl = "http://keycloak.exemplo.com";

        // Act
        keycloakConfig.setUrl(novaUrl);

        // Assert
        assertEquals(novaUrl, keycloakConfig.getUrl());
    }

    @Test
    @DisplayName("Deve permitir configuração de realm")
    void devePermitirConfiguracaoDeRealm() {
        // Arrange
        String novoRealm = "production";

        // Act
        keycloakConfig.setRealm(novoRealm);

        // Assert
        assertEquals(novoRealm, keycloakConfig.getRealm());
    }

    @Test
    @DisplayName("Deve permitir configuração de clientId")
    void devePermitirConfiguracaoDeClientId() {
        // Arrange
        String novoClientId = "prod-client";

        // Act
        keycloakConfig.setClientId(novoClientId);

        // Assert
        assertEquals(novoClientId, keycloakConfig.getClientId());
    }

    @Test
    @DisplayName("Deve permitir configuração de clientSecret")
    void devePermitirConfiguracaoDeClientSecret() {
        // Arrange
        String novoClientSecret = "novo-secret";

        // Act
        keycloakConfig.setClientSecret(novoClientSecret);

        // Assert
        assertEquals(novoClientSecret, keycloakConfig.getClientSecret());
    }
}

