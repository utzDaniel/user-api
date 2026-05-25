package br.com.user.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários de KeycloakAdminClient")
class KeycloakAdminClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private KeycloakConfig keycloakConfig;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private KeycloakAdminClient keycloakAdminClient;

    @BeforeEach
    void setUp() {
        keycloakAdminClient = new KeycloakAdminClient(restClient, keycloakConfig);
    }

    @Test
    @DisplayName("Deve obter token de admin com sucesso")
    void deveObterTokenDeAdminComSucesso() {
        // Arrange
        String expectedToken = "admin-token-123";
        Map<String, Object> response = Map.of("access_token", expectedToken);

        when(keycloakConfig.getAdminTokenUrl()).thenReturn("http://localhost:9999/token");
        when(keycloakConfig.getAdminTokenBody()).thenReturn("grant_type=client_credentials");
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(response);

        // Act
        String token = keycloakAdminClient.obtainAdminToken();

        // Assert
        assertEquals(expectedToken, token);
        verify(restClient).post();
        verify(keycloakConfig).getAdminTokenUrl();
        verify(keycloakConfig).getAdminTokenBody();
    }

    @Test
    @DisplayName("Deve lançar ApiException quando resposta não contém access_token")
    void deveLancarApiExceptionQuandoRespostaNaoContemAccessToken() {
        // Arrange
        Map<String, Object> response = Map.of();

        when(keycloakConfig.getAdminTokenUrl()).thenReturn("http://localhost:9999/token");
        when(keycloakConfig.getAdminTokenBody()).thenReturn("grant_type=client_credentials");
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(response);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> keycloakAdminClient.obtainAdminToken());
        assertEquals("Erro interno contate o administrador do Sistema", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar ApiException quando resposta é nula")
    void deveLancarApiExceptionQuandoRespostaENula() {
        // Arrange
        when(keycloakConfig.getAdminTokenUrl()).thenReturn("http://localhost:9999/token");
        when(keycloakConfig.getAdminTokenBody()).thenReturn("grant_type=client_credentials");
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(null);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> keycloakAdminClient.obtainAdminToken());
        assertEquals("Erro interno contate o administrador do Sistema", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar ApiException quando ocorre HttpClientErrorException")
    void deveLancarApiExceptionQuandoOcorreHttpClientErrorException() {
        // Arrange
        when(keycloakConfig.getAdminTokenUrl()).thenReturn("http://localhost:9999/token");
        when(keycloakConfig.getAdminTokenBody()).thenReturn("grant_type=client_credentials");
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(HttpClientErrorException.class);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> keycloakAdminClient.obtainAdminToken());
        assertEquals("Erro interno contate o administrador do Sistema", exception.getMessage());
    }

    @Test
    @DisplayName("Deve validar senha com sucesso")
    void deveValidarSenhaComSucesso() {
        // Arrange
        String username = "usuario@teste.com";
        String password = "senha123";

        when(keycloakConfig.getTokenUrl()).thenReturn("http://localhost:9999/token");
        when(keycloakConfig.getPasswordGrantBody(username, password)).thenReturn("grant_type=password");
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> keycloakAdminClient.validatePassword(username, password));
        verify(keycloakConfig).getTokenUrl();
        verify(keycloakConfig).getPasswordGrantBody(username, password);
    }

    @Test
    @DisplayName("Deve lançar ApiException com violação quando senha é inválida")
    void deveLancarApiExceptionComViolacaoQuandoSenhaEInvalida() {
        // Arrange
        String username = "usuario@teste.com";
        String password = "senhaErrada";

        when(keycloakConfig.getTokenUrl()).thenReturn("http://localhost:9999/token");
        when(keycloakConfig.getPasswordGrantBody(username, password)).thenReturn("grant_type=password");
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(HttpClientErrorException.class);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> keycloakAdminClient.validatePassword(username, password));
        assertTrue(exception.hasViolacoes());
        assertEquals("senhaAtual", exception.getViolacoes().getFirst().campo());
        assertEquals("Senha atual inválida", exception.getViolacoes().getFirst().razao());
    }

    @Test
    @DisplayName("Deve lançar ApiException badGateway quando ocorre erro genérico na validação de senha")
    void deveLancarApiExceptionBadGatewayQuandoOcorreErroGenericoNaValidacaoDeSenha() {
        // Arrange
        String username = "usuario@teste.com";
        String password = "senha123";

        when(keycloakConfig.getTokenUrl()).thenReturn("http://localhost:9999/token");
        when(keycloakConfig.getPasswordGrantBody(username, password)).thenReturn("grant_type=password");
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new RuntimeException("Erro genérico"));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> keycloakAdminClient.validatePassword(username, password));
        assertEquals("Erro ao validar senha atual", exception.getMessage());
    }

    @Test
    @DisplayName("Deve atualizar usuário com sucesso")
    void deveAtualizarUsuarioComSucesso() {
        // Arrange
        String userId = "123e4567-e89b-12d3-a456-426614174000";
        String firstName = "João";
        String lastName = "Silva";
        String email = "joao@teste.com";
        boolean emailVerified = true;
        String adminToken = "admin-token-123";

        when(keycloakConfig.getAdminTokenUrl()).thenReturn("http://localhost:9999/token");
        when(keycloakConfig.getAdminTokenBody()).thenReturn("grant_type=client_credentials");
        when(keycloakConfig.getUserAdminUrl(userId)).thenReturn("http://localhost:9999/admin/users/" + userId);

        // Mock para obter token
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("access_token", adminToken));

        // Mock para atualizar usuário
        RestClient.RequestBodyUriSpec putRequestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec putRequestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec putResponseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.put()).thenReturn(putRequestBodyUriSpec);
        when(putRequestBodyUriSpec.uri(any(URI.class))).thenReturn(putRequestBodySpec);
        when(putRequestBodySpec.header(eq("Authorization"), anyString())).thenReturn(putRequestBodySpec);
        when(putRequestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(putRequestBodySpec);
        when(putRequestBodySpec.body(any(Map.class))).thenReturn(putRequestBodySpec);
        when(putRequestBodySpec.retrieve()).thenReturn(putResponseSpec);
        when(putResponseSpec.toBodilessEntity()).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> keycloakAdminClient.updateUser(userId, firstName, lastName, email, emailVerified));
        verify(keycloakConfig).getUserAdminUrl(userId);
    }

    @Test
    @DisplayName("Deve lançar ApiException quando falha ao atualizar usuário")
    void deveLancarApiExceptionQuandoFalhaAoAtualizarUsuario() {
        // Arrange
        String userId = "123e4567-e89b-12d3-a456-426614174000";
        String adminToken = "admin-token-123";

        when(keycloakConfig.getAdminTokenUrl()).thenReturn("http://localhost:9999/token");
        when(keycloakConfig.getAdminTokenBody()).thenReturn("grant_type=client_credentials");
        when(keycloakConfig.getUserAdminUrl(userId)).thenReturn("http://localhost:9999/admin/users/" + userId);

        // Mock para obter token
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("access_token", adminToken));

        // Mock para atualizar usuário com erro
        RestClient.RequestBodyUriSpec putRequestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(restClient.put()).thenReturn(putRequestBodyUriSpec);
        when(putRequestBodyUriSpec.uri(any(URI.class))).thenThrow(new RuntimeException("Erro ao atualizar"));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> keycloakAdminClient.updateUser(userId, "Nome", "Sobrenome", "email@teste.com", true));
        assertEquals("Erro ao atualizar dados", exception.getMessage());
    }

    @Test
    @DisplayName("Deve resetar senha com sucesso")
    void deveResetarSenhaComSucesso() {
        // Arrange
        String userId = "123e4567-e89b-12d3-a456-426614174000";
        String newPassword = "novaSenha123";
        String adminToken = "admin-token-123";

        when(keycloakConfig.getAdminTokenUrl()).thenReturn("http://localhost:9999/token");
        when(keycloakConfig.getAdminTokenBody()).thenReturn("grant_type=client_credentials");
        when(keycloakConfig.getResetPasswordUrl(userId)).thenReturn("http://localhost:9999/admin/users/" + userId + "/reset-password");

        // Mock para obter token
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("access_token", adminToken));

        // Mock para resetar senha
        RestClient.RequestBodyUriSpec putRequestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec putRequestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec putResponseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.put()).thenReturn(putRequestBodyUriSpec);
        when(putRequestBodyUriSpec.uri(any(URI.class))).thenReturn(putRequestBodySpec);
        when(putRequestBodySpec.header(eq("Authorization"), anyString())).thenReturn(putRequestBodySpec);
        when(putRequestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(putRequestBodySpec);
        when(putRequestBodySpec.body(any(Map.class))).thenReturn(putRequestBodySpec);
        when(putRequestBodySpec.retrieve()).thenReturn(putResponseSpec);
        when(putResponseSpec.toBodilessEntity()).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> keycloakAdminClient.resetPassword(userId, newPassword));
        verify(keycloakConfig).getResetPasswordUrl(userId);
    }

    @Test
    @DisplayName("Deve lançar ApiException quando falha ao resetar senha")
    void deveLancarApiExceptionQuandoFalhaAoResetarSenha() {
        // Arrange
        String userId = "123e4567-e89b-12d3-a456-426614174000";
        String newPassword = "novaSenha123";
        String adminToken = "admin-token-123";

        when(keycloakConfig.getAdminTokenUrl()).thenReturn("http://localhost:9999/token");
        when(keycloakConfig.getAdminTokenBody()).thenReturn("grant_type=client_credentials");
        when(keycloakConfig.getResetPasswordUrl(userId)).thenReturn("http://localhost:9999/admin/users/" + userId + "/reset-password");

        // Mock para obter token
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("access_token", adminToken));

        // Mock para resetar senha com erro
        RestClient.RequestBodyUriSpec putRequestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(restClient.put()).thenReturn(putRequestBodyUriSpec);
        when(putRequestBodyUriSpec.uri(any(URI.class))).thenThrow(new RuntimeException("Erro ao resetar"));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> keycloakAdminClient.resetPassword(userId, newPassword));
        assertEquals("Erro ao trocar de senha", exception.getMessage());
    }
}

