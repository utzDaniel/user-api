package br.com.user.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.FieldError;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes unitários de Violacao")
class ViolacaoTest {

    @Test
    @DisplayName("Deve criar uma Violacao com campo e razão")
    void deveCriarViolacaoComCampoERazao() {
        // Arrange
        String campo = "nome";
        String razao = "Campo obrigatório";

        // Act
        Violacao violacao = new Violacao(campo, razao);

        // Assert
        assertEquals(campo, violacao.campo());
        assertEquals(razao, violacao.razao());
    }

    @Test
    @DisplayName("Deve criar uma Violacao a partir de FieldError")
    void deveCriarViolacaoAPartirDeFieldError() {
        // Arrange
        FieldError fieldError = new FieldError("usuario", "email", "Email inválido");

        // Act
        Violacao violacao = new Violacao(fieldError);

        // Assert
        assertEquals("email", violacao.campo());
        assertEquals("Email inválido", violacao.razao());
    }

    @Test
    @DisplayName("Deve permitir valores nulos")
    void devePermitirValoresNulos() {
        // Act
        Violacao violacao = new Violacao(null, null);

        // Assert
        assertNull(violacao.campo());
        assertNull(violacao.razao());
    }

    @Test
    @DisplayName("Deve comparar violações com equals")
    void deveCompararViolacoesComEquals() {
        // Arrange
        Violacao violacao1 = new Violacao("campo", "razao");
        Violacao violacao2 = new Violacao("campo", "razao");
        Violacao violacao3 = new Violacao("outro", "razao");

        // Assert
        assertEquals(violacao1, violacao2);
        assertNotEquals(violacao1, violacao3);
    }

    @Test
    @DisplayName("Deve retornar o mesmo hashCode para violações iguais")
    void deveRetornarMesmoHashCodeParaViolacoesIguais() {
        // Arrange
        Violacao violacao1 = new Violacao("campo", "razao");
        Violacao violacao2 = new Violacao("campo", "razao");

        // Assert
        assertEquals(violacao1.hashCode(), violacao2.hashCode());
    }

    @Test
    @DisplayName("Deve retornar string formatada com toString")
    void deveRetornarStringFormatadaComToString() {
        // Arrange
        Violacao violacao = new Violacao("campo", "razao");

        // Act
        String toString = violacao.toString();

        // Assert
        assertTrue(toString.contains("campo"));
        assertTrue(toString.contains("razao"));
    }
}

