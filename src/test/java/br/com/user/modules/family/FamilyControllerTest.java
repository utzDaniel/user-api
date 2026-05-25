package br.com.user.modules.family;

import br.com.user.modules.family.dto.CreateFamilyRequest;
import br.com.user.modules.family.dto.FamilyMemberResponse;
import br.com.user.modules.family.dto.FamilyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyController - Testes do controller de família")
class FamilyControllerTest {

    @Mock
    private FamilyService familyService;

    @InjectMocks
    private FamilyController familyController;

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("preferred_username", "joao.silva")
                .claim("sub", "user-id-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/user/family - Deve criar família com sucesso")
    void deveCriarFamiliaComSucesso() {
        // Arrange
        CreateFamilyRequest request = new CreateFamilyRequest("Família Silva");
        FamilyMemberResponse member = new FamilyMemberResponse(
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                false
        );
        FamilyResponse mockResponse = new FamilyResponse("Família Silva", true, List.of(member));

        when(familyService.createFamily(any(Jwt.class), eq("Família Silva")))
                .thenReturn(mockResponse);

        // Act
        ResponseEntity<FamilyResponse> response = familyController.createFamily(jwt, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Família Silva", response.getBody().nome());
        assertTrue(response.getBody().titular());
        assertEquals(1, response.getBody().membros().size());
        assertEquals("joao.silva", response.getBody().membros().getFirst().username());

        verify(familyService, times(1)).createFamily(jwt, "Família Silva");
    }

    @Test
    @DisplayName("POST /api/v1/user/family - Deve chamar service com nome correto")
    void deveChamarServiceComNomeCorreto() {
        // Arrange
        CreateFamilyRequest request = new CreateFamilyRequest("Minha Família");
        FamilyResponse mockResponse = new FamilyResponse("Minha Família", true, List.of());

        when(familyService.createFamily(any(Jwt.class), eq("Minha Família")))
                .thenReturn(mockResponse);

        // Act
        familyController.createFamily(jwt, request);

        // Assert
        verify(familyService, times(1)).createFamily(jwt, "Minha Família");
    }

    @Test
    @DisplayName("GET /api/v1/user/family - Deve retornar família do usuário com sucesso")
    void deveRetornarFamiliaDoUsuarioComSucesso() {
        // Arrange
        List<FamilyMemberResponse> membros = List.of(
                new FamilyMemberResponse("joao.silva", "João", "Silva", "joao@email.com", true, false),
                new FamilyMemberResponse("maria.silva", "Maria", "Silva", "maria@email.com", true, true)
        );
        FamilyResponse mockResponse = new FamilyResponse("Família Silva", true, membros);

        when(familyService.getFamily(any(Jwt.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<FamilyResponse> response = familyController.getFamily(jwt);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Família Silva", response.getBody().nome());
        assertTrue(response.getBody().titular());
        assertEquals(2, response.getBody().membros().size());

        verify(familyService, times(1)).getFamily(jwt);
    }

    @Test
    @DisplayName("GET /api/v1/user/family - Deve retornar família sem ser titular")
    void deveRetornarFamiliaSemSerTitular() {
        // Arrange
        List<FamilyMemberResponse> membros = List.of(
                new FamilyMemberResponse("pedro.santos", "Pedro", "Santos", "pedro@email.com", true, false),
                new FamilyMemberResponse("maria.silva", "Maria", "Silva", "maria@email.com", true, false)
        );
        FamilyResponse mockResponse = new FamilyResponse("Família Santos", false, membros);

        when(familyService.getFamily(any(Jwt.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<FamilyResponse> response = familyController.getFamily(jwt);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Família Santos", response.getBody().nome());
        assertFalse(response.getBody().titular());
        assertEquals(2, response.getBody().membros().size());

        verify(familyService, times(1)).getFamily(jwt);
    }

    @Test
    @DisplayName("POST /api/v1/user/family/members/{username} - Deve adicionar membro com sucesso")
    void deveAdicionarMembroComSucesso() {
        // Arrange
        String username = "maria.santos";
        doNothing().when(familyService).addFamilyMember(any(Jwt.class), eq(username));

        // Act
        ResponseEntity<Void> response = familyController.addFamilyMember(username, jwt);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNull(response.getBody());

        verify(familyService, times(1)).addFamilyMember(jwt, username);
    }

    @Test
    @DisplayName("POST /api/v1/user/family/members/{username} - Deve chamar service com username correto")
    void deveChamarServiceComUsernameCorretoAoAdicionar() {
        // Arrange
        String username = "carlos.oliveira";
        doNothing().when(familyService).addFamilyMember(any(Jwt.class), eq(username));

        // Act
        familyController.addFamilyMember(username, jwt);

        // Assert
        verify(familyService, times(1)).addFamilyMember(jwt, username);
    }

    @Test
    @DisplayName("DELETE /api/v1/user/family/members/{username} - Deve remover membro com sucesso")
    void deveRemoverMembroComSucesso() {
        // Arrange
        String username = "maria.santos";
        doNothing().when(familyService).removeFamilyMember(any(Jwt.class), eq(username));

        // Act
        ResponseEntity<Void> response = familyController.removeFamilyMember(username, jwt);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(familyService, times(1)).removeFamilyMember(jwt, username);
    }

    @Test
    @DisplayName("DELETE /api/v1/user/family/members/{username} - Deve chamar service com username correto")
    void deveChamarServiceComUsernameCorretoAoRemover() {
        // Arrange
        String username = "pedro.costa";
        doNothing().when(familyService).removeFamilyMember(any(Jwt.class), eq(username));

        // Act
        familyController.removeFamilyMember(username, jwt);

        // Assert
        verify(familyService, times(1)).removeFamilyMember(jwt, username);
    }

    @Test
    @DisplayName("DELETE /api/v1/user/family - Deve deletar família com sucesso")
    void deveDeletarFamiliaComSucesso() {
        // Arrange
        doNothing().when(familyService).deleteFamily(any(Jwt.class));

        // Act
        ResponseEntity<Void> response = familyController.deleteFamily(jwt);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(familyService, times(1)).deleteFamily(jwt);
    }

    @Test
    @DisplayName("DELETE /api/v1/user/family - Deve chamar service uma única vez")
    void deveChamarServiceUmaUnicaVezAoDeletar() {
        // Arrange
        doNothing().when(familyService).deleteFamily(any(Jwt.class));

        // Act
        familyController.deleteFamily(jwt);

        // Assert
        verify(familyService, times(1)).deleteFamily(jwt);
    }

    @Test
    @DisplayName("Deve processar requisições com JWT válido")
    void deveProcessarRequisicoesComJwtValido() {
        // Arrange
        FamilyResponse mockResponse = new FamilyResponse("Família Teste", true, List.of());
        when(familyService.getFamily(any(Jwt.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<FamilyResponse> response = familyController.getFamily(jwt);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(familyService).getFamily(jwt);
    }
}

