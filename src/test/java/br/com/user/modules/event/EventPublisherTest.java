package br.com.user.modules.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventPublisher - Testes do publicador de eventos")
class EventPublisherTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @InjectMocks
    private EventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<Map<String, Object>> eventCaptor;

    @BeforeEach
    void setUp() {
        // Preparação se necessária
    }

    @Test
    @DisplayName("Deve publicar evento com todos os campos corretos")
    void devePublicarEventoComTodosCampos() {
        // Arrange
        EventType tipo = EventType.USER_UPDATED;
        String userId = "user-123";
        Map<String, Object> payload = Map.of("campo1", "valor1", "campo2", 123);

        // Act
        eventPublisher.publish(tipo, userId, payload);

        // Assert
        verify(jmsTemplate, times(1)).convertAndSend(eq("events"), eventCaptor.capture());

        Map<String, Object> eventoCriado = eventCaptor.getValue();
        assertNotNull(eventoCriado, "Evento não deve ser nulo");
        assertEquals("USER_UPDATED", eventoCriado.get("type"), "Tipo deve estar correto");
        assertEquals(userId, eventoCriado.get("userId"), "UserId deve estar correto");
        assertEquals(payload, eventoCriado.get("payload"), "Payload deve estar correto");
        assertNotNull(eventoCriado.get("timestamp"), "Timestamp deve estar presente");
    }

    @Test
    @DisplayName("Deve publicar evento USER_PASSWORD_CHANGED")
    void devePublicarEventoUserPasswordChanged() {
        // Arrange
        EventType tipo = EventType.USER_PASSWORD_CHANGED;
        String userId = "user-456";
        Map<String, Object> payload = Map.of("passwordChanged", true);

        // Act
        eventPublisher.publish(tipo, userId, payload);

        // Assert
        verify(jmsTemplate, times(1)).convertAndSend(eq("events"), eventCaptor.capture());

        Map<String, Object> eventoCriado = eventCaptor.getValue();
        assertEquals("USER_PASSWORD_CHANGED", eventoCriado.get("type"));
        assertEquals(userId, eventoCriado.get("userId"));
    }

    @Test
    @DisplayName("Deve publicar evento FAMILY_CREATED")
    void devePublicarEventoFamilyCreated() {
        // Arrange
        EventType tipo = EventType.FAMILY_CREATED;
        String userId = "user-789";
        Map<String, Object> payload = Map.of("familyId", "family-123", "familyName", "Silva");

        // Act
        eventPublisher.publish(tipo, userId, payload);

        // Assert
        verify(jmsTemplate, times(1)).convertAndSend(eq("events"), eventCaptor.capture());

        Map<String, Object> eventoCriado = eventCaptor.getValue();
        assertEquals("FAMILY_CREATED", eventoCriado.get("type"));
        assertEquals(userId, eventoCriado.get("userId"));
        assertEquals(payload, eventoCriado.get("payload"));
    }

    @Test
    @DisplayName("Deve publicar evento FAMILY_MEMBER_REMOVED")
    void devePublicarEventoFamilyMemberRemoved() {
        // Arrange
        EventType tipo = EventType.FAMILY_MEMBER_REMOVED;
        String userId = "user-999";
        Map<String, Object> payload = Map.of("memberId", "member-123", "familyId", "family-456");

        // Act
        eventPublisher.publish(tipo, userId, payload);

        // Assert
        verify(jmsTemplate, times(1)).convertAndSend(eq("events"), eventCaptor.capture());

        Map<String, Object> eventoCriado = eventCaptor.getValue();
        assertEquals("FAMILY_MEMBER_REMOVED", eventoCriado.get("type"));
        assertEquals(userId, eventoCriado.get("userId"));
    }

    @Test
    @DisplayName("Deve publicar evento com payload vazio")
    void devePublicarEventoComPayloadVazio() {
        // Arrange
        EventType tipo = EventType.USER_UPDATED;
        String userId = "user-empty";
        Map<String, Object> payload = Map.of();

        // Act
        eventPublisher.publish(tipo, userId, payload);

        // Assert
        verify(jmsTemplate, times(1)).convertAndSend(eq("events"), eventCaptor.capture());

        Map<String, Object> eventoCriado = eventCaptor.getValue();
        assertNotNull(eventoCriado);
        assertEquals(payload, eventoCriado.get("payload"));
        assertTrue(((Map<?, ?>) eventoCriado.get("payload")).isEmpty());
    }

    @Test
    @DisplayName("Deve publicar evento com payload de string simples")
    void devePublicarEventoComPayloadString() {
        // Arrange
        EventType tipo = EventType.USER_UPDATED;
        String userId = "user-string";
        String payload = "informacao-textual";

        // Act
        eventPublisher.publish(tipo, userId, payload);

        // Assert
        verify(jmsTemplate, times(1)).convertAndSend(eq("events"), eventCaptor.capture());

        Map<String, Object> eventoCriado = eventCaptor.getValue();
        assertEquals("informacao-textual", eventoCriado.get("payload"));
    }

    @Test
    @DisplayName("Deve sempre usar a fila 'events'")
    void deveSempreUsarFilaEvents() {
        // Arrange
        EventType tipo = EventType.USER_UPDATED;
        String userId = "user-123";
        Object payload = Map.of();

        // Act
        eventPublisher.publish(tipo, userId, payload);

        // Assert
        verify(jmsTemplate).convertAndSend(eq("events"), any(Map.class));
    }

    @Test
    @DisplayName("Deve criar evento com estrutura esperada")
    void deveCriarEventoComEstruturaEsperada() {
        // Arrange
        EventType tipo = EventType.FAMILY_CREATED;
        String userId = "user-structure";
        Object payload = Map.of("teste", "valor");

        // Act
        eventPublisher.publish(tipo, userId, payload);

        // Assert
        verify(jmsTemplate).convertAndSend(eq("events"), eventCaptor.capture());

        Map<String, Object> evento = eventCaptor.getValue();

        // Verifica que o evento tem exatamente 4 campos
        assertEquals(4, evento.size(), "Evento deve ter 4 campos");
        assertTrue(evento.containsKey("type"), "Deve conter campo 'type'");
        assertTrue(evento.containsKey("timestamp"), "Deve conter campo 'timestamp'");
        assertTrue(evento.containsKey("userId"), "Deve conter campo 'userId'");
        assertTrue(evento.containsKey("payload"), "Deve conter campo 'payload'");
    }
}


