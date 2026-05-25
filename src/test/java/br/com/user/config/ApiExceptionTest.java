package br.com.user.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes unitários de ApiException")
class ApiExceptionTest {

    @Test
    @DisplayName("Deve criar uma ApiException com todos os parâmetros")
    void deveCriarApiExceptionComTodosParametros() {
        // Arrange
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String error = "Erro de validação";
        String message = "Dados inválidos";
        List<Violacao> violacoes = List.of(new Violacao("campo", "razão"));

        // Act
        ApiException exception = new ApiException(status, error, message, violacoes);

        // Assert
        assertEquals(status, exception.getStatus());
        assertEquals(error, exception.getError());
        assertEquals(message, exception.getMessage());
        assertEquals(violacoes, exception.getViolacoes());
        assertTrue(exception.hasViolacoes());
    }

    @Test
    @DisplayName("Deve criar uma ApiException com lista de violações nula")
    void deveCriarApiExceptionComViolacoesNulas() {
        // Arrange & Act
        ApiException exception = new ApiException(HttpStatus.BAD_REQUEST, "Erro", "Mensagem", null);

        // Assert
        assertNotNull(exception.getViolacoes());
        assertTrue(exception.getViolacoes().isEmpty());
        assertFalse(exception.hasViolacoes());
    }

    @Test
    @DisplayName("Deve retornar false quando não houver violações")
    void deveRetornarFalseQuandoNaoHouverViolacoes() {
        // Arrange & Act
        ApiException exception = new ApiException(HttpStatus.BAD_REQUEST, "Erro", "Mensagem", new ArrayList<>());

        // Assert
        assertFalse(exception.hasViolacoes());
    }

    @Test
    @DisplayName("Deve criar ApiException notFound com método estático")
    void deveCriarNotFoundComMetodoEstatico() {
        // Arrange
        String message = "Recurso não encontrado";

        // Act
        ApiException exception = ApiException.notFound(message);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Não Encontrado", exception.getError());
        assertEquals(message, exception.getMessage());
        assertFalse(exception.hasViolacoes());
    }

    @Test
    @DisplayName("Deve criar ApiException badRequest com lista de violações")
    void deveCriarBadRequestComViolacoes() {
        // Arrange
        List<Violacao> violacoes = List.of(
                new Violacao("campo1", "razão1"),
                new Violacao("campo2", "razão2")
        );

        // Act
        ApiException exception = ApiException.badRequest(violacoes);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Requisição Inválida", exception.getError());
        assertEquals("Erro de validação", exception.getMessage());
        assertEquals(violacoes, exception.getViolacoes());
        assertTrue(exception.hasViolacoes());
    }

    @Test
    @DisplayName("Deve criar ApiException badRequest com mensagem customizada")
    void deveCriarBadRequestComMensagemCustomizada() {
        // Arrange
        String message = "Dados inválidos";

        // Act
        ApiException exception = ApiException.badRequest(message);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Requisição Inválida", exception.getError());
        assertEquals(message, exception.getMessage());
        assertFalse(exception.hasViolacoes());
    }

    @Test
    @DisplayName("Deve criar ApiException badGateway com método estático")
    void deveCriarBadGatewayComMetodoEstatico() {
        // Arrange
        String message = "Erro no Keycloak";

        // Act
        ApiException exception = ApiException.badGateway(message);

        // Assert
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("Erro no keycloak", exception.getError());
        assertEquals(message, exception.getMessage());
        assertFalse(exception.hasViolacoes());
    }

    @Test
    @DisplayName("Deve criar ApiException forbidden com método estático")
    void deveCriarForbiddenComMetodoEstatico() {
        // Arrange
        String message = "Acesso não permitido";

        // Act
        ApiException exception = ApiException.forbiden(message);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Acesso negado", exception.getError());
        assertEquals(message, exception.getMessage());
        assertFalse(exception.hasViolacoes());
    }
}

