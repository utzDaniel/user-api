package br.com.user.modules.user;

import br.com.user.config.ApiException;
import br.com.user.config.KeycloakAdminClient;
import br.com.user.config.KeycloakConfig;
import br.com.user.modules.event.EventPublisher;
import br.com.user.modules.event.EventType;
import br.com.user.modules.family.dto.FamilyMemberResponse;
import br.com.user.modules.user.dto.KeycloakUserDto;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService - Testes do serviço de usuário")
class UserServiceTest {

    @Mock
    private KeycloakUserDao keycloakUserDao;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private KeycloakConfig keycloakConfig;

    @InjectMocks
    private UserService userService;

    private Jwt jwt;
    private KeycloakUserDto keycloakUserDto;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("preferred_username", "joao.silva")
                .claim("sub", "user-id-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        keycloakUserDto = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                null,
                null,
                false
        );

        when(keycloakConfig.getRealm()).thenReturn("development");
    }

    @Test
    @DisplayName("Deve retornar dados do usuário com sucesso")
    void deveRetornarDadosUsuarioComSucesso() {
        // Arrange
        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(keycloakUserDto));

        // Act
        UserResponse response = userService.getUser(jwt);

        // Assert
        assertNotNull(response);
        assertEquals("João", response.nome());
        assertEquals("Silva", response.sobrenome());
        assertEquals("joao@email.com", response.email());
        assertTrue(response.emailVerificado());

        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não for encontrado")
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        // Arrange
        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.empty());

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> userService.getUser(jwt));

        assertEquals("User não encontrado", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
    }

    @Test
    @DisplayName("Deve atualizar usuário com mesmo email sem mudar emailVerified")
    void deveAtualizarUsuarioComMesmoEmail() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("João Pedro", "Silva Santos", "joao@email.com");

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(keycloakUserDto));
        doNothing().when(keycloakAdminClient).updateUser(anyString(), anyString(), anyString(), anyString(), anyBoolean());
        doNothing().when(eventPublisher).publish(any(EventType.class), anyString(), any());

        // Act
        UserResponse response = userService.updateUser(jwt, request);

        // Assert
        assertNotNull(response);
        assertEquals("João Pedro", response.nome());
        assertEquals("Silva Santos", response.sobrenome());
        assertEquals("joao@email.com", response.email());
        assertTrue(response.emailVerificado(), "Email verificado deve permanecer true quando email não muda");

        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(keycloakUserDao, never()).existsByEmail(anyString());
        verify(keycloakAdminClient, times(1)).updateUser("user-id-123", "João Pedro", "Silva Santos", "joao@email.com", true);
        verify(eventPublisher, times(1)).publish(EventType.USER_UPDATED, "user-id-123", request);
    }

    @Test
    @DisplayName("Deve atualizar usuário com novo email e setar emailVerified como false")
    void deveAtualizarUsuarioComNovoEmail() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("João", "Silva", "novoemail@email.com");

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(keycloakUserDto));
        when(keycloakUserDao.existsByEmail("novoemail@email.com"))
                .thenReturn(Optional.of(true));  // Email existe, mas o orElseThrow verifica se Optional está vazio
        doNothing().when(keycloakAdminClient).updateUser(anyString(), anyString(), anyString(), anyString(), anyBoolean());
        doNothing().when(eventPublisher).publish(any(EventType.class), anyString(), any());

        // Act
        UserResponse response = userService.updateUser(jwt, request);

        // Assert
        assertNotNull(response);
        assertEquals("João", response.nome());
        assertEquals("Silva", response.sobrenome());
        assertEquals("novoemail@email.com", response.email());
        assertFalse(response.emailVerificado(), "Email verificado deve ser false quando email muda");

        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(keycloakUserDao, times(1)).existsByEmail("novoemail@email.com");
        verify(keycloakAdminClient, times(1)).updateUser("user-id-123", "João", "Silva", "novoemail@email.com", false);
        verify(eventPublisher, times(1)).publish(EventType.USER_UPDATED, "user-id-123", request);
    }

    @Test
    @DisplayName("Deve lançar exceção quando verificação de email falha")
    void deveLancarExcecaoQuandoVerificacaoEmailFalha() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("João", "Silva", "emailemuso@email.com");

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(keycloakUserDto));
        when(keycloakUserDao.existsByEmail("emailemuso@email.com"))
                .thenReturn(Optional.empty());  // orElseThrow lança quando Optional está vazio

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> userService.updateUser(jwt, request));

        assertNotNull(exception.getViolacoes());
        assertEquals(1, exception.getViolacoes().size());
        assertEquals("email", exception.getViolacoes().getFirst().campo());
        assertEquals("E-mail já utilizado", exception.getViolacoes().getFirst().razao());

        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(keycloakUserDao, times(1)).existsByEmail("emailemuso@email.com");
        verify(keycloakAdminClient, never()).updateUser(anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve alterar senha com sucesso")
    void deveAlterarSenhaComSucesso() {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest("senha123", "novaSenha123", "novaSenha123");

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(keycloakUserDto));
        doNothing().when(keycloakAdminClient).validatePassword(anyString(), anyString());
        doNothing().when(keycloakAdminClient).resetPassword(anyString(), anyString());
        doNothing().when(eventPublisher).publish(any(EventType.class), anyString(), any());

        // Act
        userService.changePassword(jwt, request);

        // Assert
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(keycloakAdminClient, times(1)).validatePassword("joao.silva", "senha123");
        verify(keycloakAdminClient, times(1)).resetPassword("user-id-123", "novaSenha123");
        verify(eventPublisher, times(1)).publish(EventType.USER_PASSWORD_CHANGED, "user-id-123", "joao.silva");
    }

    @Test
    @DisplayName("Deve lançar exceção quando nova senha é diferente da confirmação")
    void deveLancarExcecaoQuandoSenhasDiferentes() {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest("senha123", "novaSenha123", "senhasDiferentes");

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> userService.changePassword(jwt, request));

        assertNotNull(exception.getViolacoes());
        assertEquals(2, exception.getViolacoes().size());

        boolean temErroNovaSenha = exception.getViolacoes().stream()
                .anyMatch(v -> v.campo().equals("novaSenha") && v.razao().equals("Nova Senha diferente da confirmação"));
        boolean temErroConfirmarSenha = exception.getViolacoes().stream()
                .anyMatch(v -> v.campo().equals("confirmarNovaSenha") && v.razao().equals("Confirmação de senha diferente da nova senha"));

        assertTrue(temErroNovaSenha, "Deve conter erro para novaSenha");
        assertTrue(temErroConfirmarSenha, "Deve conter erro para confirmarNovaSenha");

        verify(keycloakUserDao, never()).findByRealmAndUsername(anyString(), anyString());
        verify(keycloakAdminClient, never()).validatePassword(anyString(), anyString());
        verify(keycloakAdminClient, never()).resetPassword(anyString(), anyString());
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve propagar exceção quando validação de senha falha")
    void devePropararExcecaoQuandoValidacaoSenhaFalha() {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest("senhaErrada", "novaSenha123", "novaSenha123");

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(keycloakUserDto));
        doThrow(ApiException.forbiden("Senha atual inválida"))
                .when(keycloakAdminClient).validatePassword("joao.silva", "senhaErrada");

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> userService.changePassword(jwt, request));

        assertEquals("Senha atual inválida", exception.getMessage());

        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(keycloakAdminClient, times(1)).validatePassword("joao.silva", "senhaErrada");
        verify(keycloakAdminClient, never()).resetPassword(anyString(), anyString());
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve buscar usuário usando realm e username do JWT")
    void deveBuscarUsuarioComRealmEUsername() {
        // Arrange
        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(keycloakUserDto));

        // Act
        userService.getUser(jwt);

        // Assert
        verify(keycloakConfig, times(1)).getRealm();
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
    }

    @Test
    @DisplayName("Deve retornar lista de usuários sem família com sucesso")
    void deveRetornarListaUsuariosSemFamiliaComSucesso() {
        // Arrange
        List<KeycloakUserDto> mockUsers = List.of(
                new KeycloakUserDto("user-id-1", "maria123", "Maria", "Santos", "maria@email.com", true, null, null, false),
                new KeycloakUserDto("user-id-2", "pedro456", "Pedro", "Oliveira", "pedro@email.com", false, null, null, false)
        );

        when(keycloakUserDao.findUsersWithoutFamily("development")).thenReturn(mockUsers);

        // Act
        List<FamilyMemberResponse> result = userService.getUsersWithoutFamily();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        FamilyMemberResponse firstUser = result.getFirst();
        assertEquals("maria123", firstUser.username());
        assertEquals("Maria", firstUser.nome());
        assertEquals("Santos", firstUser.sobrenome());
        assertEquals("maria@email.com", firstUser.email());
        assertTrue(firstUser.emailVerified());
        assertTrue(firstUser.ehDeletavel());

        FamilyMemberResponse secondUser = result.get(1);
        assertEquals("pedro456", secondUser.username());
        assertEquals("Pedro", secondUser.nome());
        assertEquals("Oliveira", secondUser.sobrenome());
        assertEquals("pedro@email.com", secondUser.email());
        assertFalse(secondUser.emailVerified());
        assertTrue(secondUser.ehDeletavel());

        verify(keycloakConfig, times(1)).getRealm();
        verify(keycloakUserDao, times(1)).findUsersWithoutFamily("development");
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há usuários sem família")
    void deveRetornarListaVaziaQuandoNaoHaUsuariosSemFamilia() {
        // Arrange
        when(keycloakUserDao.findUsersWithoutFamily("development")).thenReturn(List.of());

        // Act
        List<FamilyMemberResponse> result = userService.getUsersWithoutFamily();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(keycloakConfig, times(1)).getRealm();
        verify(keycloakUserDao, times(1)).findUsersWithoutFamily("development");
    }

    @Test
    @DisplayName("Deve mapear corretamente todos os campos para FamilyMemberResponse")
    void deveMappearCorretamenteCamposParaFamilyMemberResponse() {
        // Arrange
        KeycloakUserDto userDto = new KeycloakUserDto(
                "user-id-123",
                "carlos.souza",
                "Carlos",
                "Souza",
                "carlos@email.com",
                true,
                null,
                null,
                false
        );

        when(keycloakUserDao.findUsersWithoutFamily("development")).thenReturn(List.of(userDto));

        // Act
        List<FamilyMemberResponse> result = userService.getUsersWithoutFamily();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        FamilyMemberResponse response = result.getFirst();
        assertEquals("carlos.souza", response.username());
        assertEquals("Carlos", response.nome());
        assertEquals("Souza", response.sobrenome());
        assertEquals("carlos@email.com", response.email());
        assertTrue(response.emailVerified());
        assertTrue(response.ehDeletavel());

        verify(keycloakUserDao, times(1)).findUsersWithoutFamily("development");
    }
}

