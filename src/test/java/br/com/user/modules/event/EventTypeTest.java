package br.com.user.modules.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EventType - Testes do enum de tipos de eventos")
class EventTypeTest {

    @Test
    @DisplayName("Deve conter todos os tipos de eventos esperados")
    void deveConterTodosTiposDeEventos() {
        EventType[] tipos = EventType.values();

        assertEquals(6, tipos.length, "Deve haver exatamente 6 tipos de eventos");
        assertNotNull(EventType.valueOf("USER_UPDATED"));
        assertNotNull(EventType.valueOf("USER_PASSWORD_CHANGED"));
        assertNotNull(EventType.valueOf("FAMILY_CREATED"));
        assertNotNull(EventType.valueOf("FAMILY_MEMBER_ADDED"));
        assertNotNull(EventType.valueOf("FAMILY_MEMBER_REMOVED"));
        assertNotNull(EventType.valueOf("FAMILY_DELETED"));
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar tipo inexistente")
    void deveLancarExcecaoAoBuscarTipoInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> EventType.valueOf("TIPO_INEXISTENTE"));
    }

}

