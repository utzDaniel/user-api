package br.com.user.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes unitários de TimestampUtils")
class TimestampUtilsTest {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Test
    @DisplayName("Deve retornar timestamp no formato ISO 8601")
    void deveRetornarTimestampNoFormatoISO8601() {
        // Act
        String timestamp = TimestampUtils.now();

        // Assert
        assertNotNull(timestamp);
        assertDoesNotThrow(() -> LocalDateTime.parse(timestamp, ISO_FORMATTER));
    }

    @Test
    @DisplayName("Deve retornar timestamp com padrão correto")
    void deveRetornarTimestampComPadraoCorreto() {
        // Act
        String timestamp = TimestampUtils.now();

        // Assert
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
    }

    @Test
    @DisplayName("Deve retornar timestamp em UTC")
    void deveRetornarTimestampEmUTC() {
        // Act
        String timestamp = TimestampUtils.now();
        LocalDateTime parsedTimestamp = LocalDateTime.parse(timestamp, ISO_FORMATTER);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // Assert
        // Verificar que a diferença é menor que 1 segundo
        long differenceInSeconds = Math.abs(parsedTimestamp.toEpochSecond(ZoneOffset.UTC) - now.toEpochSecond(ZoneOffset.UTC));
        assertTrue(differenceInSeconds < 1);
    }

    @Test
    @DisplayName("Não deve permitir instanciação da classe utilitária")
    void naoDevePermitirInstanciacaoDaClasseUtilitaria() {
        // Act & Assert
        try {
            var constructor = TimestampUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            fail("Deveria ter lançado UnsupportedOperationException");
        } catch (Exception e) {
            // A exceção real está na causa
            Throwable cause = e.getCause();
            assertNotNull(cause);
            assertTrue(cause instanceof UnsupportedOperationException);
            assertEquals("Classe utilitária não pode ser instanciada", cause.getMessage());
        }
    }
}

