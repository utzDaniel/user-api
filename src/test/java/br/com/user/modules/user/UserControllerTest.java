package br.com.user.modules.user;

import br.com.user.modules.family.dto.FamilyMemberResponse;
import br.com.user.modules.user.dto.PasswordChangeRequest;
import br.com.user.modules.user.dto.UserResponse;
import br.com.user.modules.user.dto.UserUpdateRequest;
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
@DisplayName("UserController - Testes do controller de usuário")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

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
    @DisplayName("GET /api/v1/user - Deve retornar dados do usuário autenticado")
    void deveRetornarDadosUsuarioAutenticado() {
        // Arrange
        UserResponse mockResponse = new UserResponse("João", "Silva", "joao@email.com", true);
        when(userService.getUser(any(Jwt.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<UserResponse> response = userController.getUser(jwt);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("João", response.getBody().nome());
        assertEquals("Silva", response.getBody().sobrenome());
        assertEquals("joao@email.com", response.getBody().email());
        assertTrue(response.getBody().emailVerificado());

        verify(userService, times(1)).getUser(jwt);
    }

    @Test
    @DisplayName("PUT /api/v1/user - Deve atualizar dados do usuário com sucesso")
    void deveAtualizarDadosUsuarioComSucesso() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("Maria", "Santos", "maria@email.com");
        UserResponse mockResponse = new UserResponse("Maria", "Santos", "maria@email.com", false);

        when(userService.updateUser(any(Jwt.class), eq(request))).thenReturn(mockResponse);

        // Act
        ResponseEntity<UserResponse> response = userController.updateUser(jwt, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Maria", response.getBody().nome());
        assertEquals("Santos", response.getBody().sobrenome());
        assertEquals("maria@email.com", response.getBody().email());
        assertFalse(response.getBody().emailVerificado());

        verify(userService, times(1)).updateUser(jwt, request);
    }

    @Test
    @DisplayName("PUT /api/v1/user - Deve chamar service com dados corretos")
    void deveChamarServiceComDadosCorretos() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("José", "Oliveira", "jose@email.com");
        UserResponse mockResponse = new UserResponse("José", "Oliveira", "jose@email.com", true);

        when(userService.updateUser(any(Jwt.class), eq(request))).thenReturn(mockResponse);

        // Act
        userController.updateUser(jwt, request);

        // Assert
        verify(userService, times(1)).updateUser(jwt, request);
    }

    @Test
    @DisplayName("PUT /api/v1/user/password - Deve alterar senha com sucesso")
    void deveAlterarSenhaComSucesso() {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest("senha123", "novaSenha123", "novaSenha123");
        doNothing().when(userService).changePassword(any(Jwt.class), eq(request));

        // Act
        ResponseEntity<Void> response = userController.changePassword(jwt, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(userService, times(1)).changePassword(jwt, request);
    }

    @Test
    @DisplayName("PUT /api/v1/user/password - Deve chamar service com senha correta")
    void deveChamarServiceComSenhaCorreta() {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest("atual123", "nova456", "nova456");
        doNothing().when(userService).changePassword(any(Jwt.class), eq(request));

        // Act
        userController.changePassword(jwt, request);

        // Assert
        verify(userService, times(1)).changePassword(jwt, request);
    }

    @Test
    @DisplayName("Deve  processar requisições com JWT válido")
    void deveProcessarRequisicoesComJwtValido() {
        // Arrange
        UserResponse mockResponse = new UserResponse("Carlos", "Souza", "carlos@email.com", true);
        when(userService.getUser(any(Jwt.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<UserResponse> response = userController.getUser(jwt);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService).getUser(jwt);
    }

    @Test
    @DisplayName("GET /api/v1/user/lista - Deve retornar lista de usuários sem família")
    void deveRetornarListaUsuariosSemFamilia() {
        // Arrange
        List<FamilyMemberResponse> mockList = List.of(
                new FamilyMemberResponse("maria123", "Maria", "Santos", "maria@email.com", true, true),
                new FamilyMemberResponse("pedro456", "Pedro", "Oliveira", "pedro@email.com", false, true)
        );
        when(userService.getUsersWithoutFamily()).thenReturn(mockList);

        // Act
        ResponseEntity<List<FamilyMemberResponse>> response = userController.getUsersWithoutFamily();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("maria123", response.getBody().getFirst().username());
        assertEquals("Maria", response.getBody().getFirst().nome());
        assertEquals("Santos", response.getBody().getFirst().sobrenome());
        assertEquals("maria@email.com", response.getBody().getFirst().email());
        assertTrue(response.getBody().getFirst().emailVerified());
        assertTrue(response.getBody().getFirst().ehDeletavel());

        verify(userService, times(1)).getUsersWithoutFamily();
    }

    @Test
    @DisplayName("GET /api/v1/user/lista - Deve retornar lista vazia quando não há usuários sem família")
    void deveRetornarListaVaziaQuandoNaoHaUsuariosSemFamilia() {
        // Arrange
        when(userService.getUsersWithoutFamily()).thenReturn(List.of());

        // Act
        ResponseEntity<List<FamilyMemberResponse>> response = userController.getUsersWithoutFamily();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(userService, times(1)).getUsersWithoutFamily();
    }
}


