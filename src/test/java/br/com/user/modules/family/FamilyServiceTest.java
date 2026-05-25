package br.com.user.modules.family;

import br.com.user.config.ApiException;
import br.com.user.config.KeycloakConfig;
import br.com.user.modules.event.EventPublisher;
import br.com.user.modules.event.EventType;
import br.com.user.modules.family.dto.FamilyMemberResponse;
import br.com.user.modules.family.dto.FamilyResponse;
import br.com.user.modules.user.KeycloakUserDao;
import br.com.user.modules.user.dto.KeycloakUserDto;
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
@DisplayName("FamilyService - Testes do serviço de família")
class FamilyServiceTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private KeycloakUserDao keycloakUserDao;

    @Mock
    private KeycloakConfig keycloakConfig;

    @InjectMocks
    private FamilyService familyService;

    private Jwt jwt;
    private KeycloakUserDto holderUserDto;
    private KeycloakUserDto memberUserDto;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("preferred_username", "joao.silva")
                .claim("sub", "user-id-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        holderUserDto = new KeycloakUserDto(
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

        memberUserDto = new KeycloakUserDto(
                "user-id-456",
                "maria.santos",
                "Maria",
                "Santos",
                "maria@email.com",
                true,
                1L,
                "Família Silva",
                false
        );

        when(keycloakConfig.getRealm()).thenReturn("development");
    }

    @Test
    @DisplayName("Deve criar família com sucesso")
    void deveCriarFamiliaComSucesso() {
        // Arrange
        String nomeFamilia = "Família Silva";
        FamilyEntity familyEntity = FamilyEntity.builder()
                .id(1L)
                .holderId("user-id-123")
                .name(nomeFamilia)
                .build();

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderUserDto));
        when(familyRepository.save(any(FamilyEntity.class))).thenReturn(familyEntity);
        when(familyMemberRepository.save(any(FamilyMemberEntity.class)))
                .thenReturn(FamilyMemberEntity.builder().build());
        doNothing().when(eventPublisher).publish(any(EventType.class), anyString(), any());

        // Act
        FamilyResponse response = familyService.createFamily(jwt, nomeFamilia);

        // Assert
        assertNotNull(response);
        assertEquals(nomeFamilia, response.nome());
        assertTrue(response.titular());
        assertEquals(1, response.membros().size());
        assertEquals("joao.silva", response.membros().getFirst().username());
        assertEquals("João", response.membros().getFirst().nome());
        assertEquals("Silva", response.membros().getFirst().sobrenome());

        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(familyRepository, times(1)).save(any(FamilyEntity.class));
        verify(familyMemberRepository, times(1)).save(any(FamilyMemberEntity.class));
        verify(eventPublisher, times(1)).publish(EventType.FAMILY_CREATED, "user-id-123", nomeFamilia);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar família quando usuário já pertence a outra família")
    void deveLancarExcecaoAoCriarFamiliaQuandoUsuarioJaPertenceAFamilia() {
        // Arrange
        KeycloakUserDto userComFamilia = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Existente",
                true
        );

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(userComFamilia));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> familyService.createFamily(jwt, "Nova Família"));

        assertEquals("Usuário já pertence a uma família", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(familyRepository, never()).save(any(FamilyEntity.class));
        verify(familyMemberRepository, never()).save(any(FamilyMemberEntity.class));
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar família quando perfil não for encontrado")
    void deveLancarExcecaoAoCriarFamiliaQuandoPerfilNaoEncontrado() {
        // Arrange
        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.empty());

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> familyService.createFamily(jwt, "Família Teste"));

        assertEquals("Perfil não encontrado", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(familyRepository, never()).save(any(FamilyEntity.class));
    }

    @Test
    @DisplayName("Deve retornar família do usuário com sucesso")
    void deveRetornarFamiliaDoUsuarioComSucesso() {
        // Arrange
        KeycloakUserDto holderComFamilia = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                true
        );

        List<KeycloakUserDto> membros = List.of(holderComFamilia, memberUserDto);

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderComFamilia));
        when(keycloakUserDao.findByFamilyId(1L)).thenReturn(membros);

        // Act
        FamilyResponse response = familyService.getFamily(jwt);

        // Assert
        assertNotNull(response);
        assertEquals("Família Silva", response.nome());
        assertTrue(response.titular());
        assertEquals(2, response.membros().size());

        FamilyMemberResponse firstMember = response.membros().getFirst();
        assertEquals("joao.silva", firstMember.username());
        assertFalse(firstMember.ehDeletavel(), "Titular não deve poder ser deletado");

        FamilyMemberResponse secondMember = response.membros().get(1);
        assertEquals("maria.santos", secondMember.username());
        assertTrue(secondMember.ehDeletavel(), "Membro não-titular deve poder ser deletado");

        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(keycloakUserDao, times(1)).findByFamilyId(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar família quando usuário não pertence a nenhuma")
    void deveLancarExcecaoAoBuscarFamiliaQuandoUsuarioNaoPertence() {
        // Arrange
        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderUserDto));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> familyService.getFamily(jwt));

        assertEquals("Usuário não pertence a uma família", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(keycloakUserDao, never()).findByFamilyId(anyLong());
    }

    @Test
    @DisplayName("Deve adicionar membro à família com sucesso")
    void deveAdicionarMembroAFamiliaComSucesso() {
        // Arrange
        KeycloakUserDto holderComFamilia = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                true
        );

        KeycloakUserDto novoMembro = new KeycloakUserDto(
                "user-id-789",
                "carlos.oliveira",
                "Carlos",
                "Oliveira",
                "carlos@email.com",
                true,
                null,
                null,
                false
        );

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderComFamilia));
        when(keycloakUserDao.findByRealmAndUsername("development", "carlos.oliveira"))
                .thenReturn(Optional.of(novoMembro));
        when(familyMemberRepository.save(any(FamilyMemberEntity.class)))
                .thenReturn(FamilyMemberEntity.builder().build());
        doNothing().when(eventPublisher).publish(any(EventType.class), anyString(), any());

        // Act
        familyService.addFamilyMember(jwt, "carlos.oliveira");

        // Assert
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "carlos.oliveira");
        verify(familyMemberRepository, times(1)).save(any(FamilyMemberEntity.class));
        verify(eventPublisher, times(1)).publish(EventType.FAMILY_MEMBER_ADDED, "user-id-123", "carlos.oliveira");
    }

    @Test
    @DisplayName("Deve lançar exceção ao adicionar membro quando titular não pertence a família")
    void deveLancarExcecaoAoAdicionarMembroQuandoTitularNaoPertenceAFamilia() {
        // Arrange
        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderUserDto));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> familyService.addFamilyMember(jwt, "carlos.oliveira"));

        assertEquals("Usuário não pertence a uma família", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(familyMemberRepository, never()).save(any(FamilyMemberEntity.class));
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao adicionar membro quando usuário não é titular")
    void deveLancarExcecaoAoAdicionarMembroQuandoUsuarioNaoETitular() {
        // Arrange
        KeycloakUserDto membroNaoTitular = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                false
        );

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(membroNaoTitular));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> familyService.addFamilyMember(jwt, "carlos.oliveira"));

        assertEquals("Somente o titular pode adicionar membros", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(familyMemberRepository, never()).save(any(FamilyMemberEntity.class));
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao adicionar membro quando usuário não existe")
    void deveLancarExcecaoAoAdicionarMembroQuandoUsuarioNaoExiste() {
        // Arrange
        KeycloakUserDto holderComFamilia = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                true
        );

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderComFamilia));
        when(keycloakUserDao.findByRealmAndUsername("development", "usuario.inexistente"))
                .thenReturn(Optional.empty());

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> familyService.addFamilyMember(jwt, "usuario.inexistente"));

        assertEquals("Usuário não encontrado", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "usuario.inexistente");
        verify(familyMemberRepository, never()).save(any(FamilyMemberEntity.class));
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao adicionar membro quando ele já pertence a outra família")
    void deveLancarExcecaoAoAdicionarMembroQuandoEleJaPertenceAFamilia() {
        // Arrange
        KeycloakUserDto holderComFamilia = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                true
        );

        KeycloakUserDto membroComFamilia = new KeycloakUserDto(
                "user-id-789",
                "carlos.oliveira",
                "Carlos",
                "Oliveira",
                "carlos@email.com",
                true,
                2L,
                "Família Oliveira",
                false
        );

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderComFamilia));
        when(keycloakUserDao.findByRealmAndUsername("development", "carlos.oliveira"))
                .thenReturn(Optional.of(membroComFamilia));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> familyService.addFamilyMember(jwt, "carlos.oliveira"));

        assertEquals("Usuário já pertence a uma família", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "carlos.oliveira");
        verify(familyMemberRepository, never()).save(any(FamilyMemberEntity.class));
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve remover membro da família com sucesso")
    void deveRemoverMembroDaFamiliaComSucesso() {
        // Arrange
        KeycloakUserDto holderComFamilia = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                true
        );

        FamilyMemberEntity memberEntity = FamilyMemberEntity.builder()
                .id(1L)
                .familyId(1L)
                .userId("user-id-456")
                .build();

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderComFamilia));
        when(familyMemberRepository.findByUsername("maria.santos"))
                .thenReturn(Optional.of(memberEntity));
        doNothing().when(familyMemberRepository).delete(any(FamilyMemberEntity.class));
        doNothing().when(eventPublisher).publish(any(EventType.class), anyString(), any());

        // Act
        familyService.removeFamilyMember(jwt, "maria.santos");

        // Assert
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(familyMemberRepository, times(1)).findByUsername("maria.santos");
        verify(familyMemberRepository, times(1)).delete(memberEntity);
        verify(eventPublisher, times(1)).publish(EventType.FAMILY_MEMBER_REMOVED, "user-id-123", "maria.santos");
    }

    @Test
    @DisplayName("Deve lançar exceção quando titular tenta remover a si mesmo")
    void deveLancarExcecaoQuandoTitularTentaRemoverASiMesmo() {
        // Arrange
        KeycloakUserDto holderComFamilia = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                true
        );

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderComFamilia));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> familyService.removeFamilyMember(jwt, "joao.silva"));

        assertEquals("O titular não pode remover a si mesmo", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(familyMemberRepository, never()).findByUsername(anyString());
        verify(familyMemberRepository, never()).delete(any(FamilyMemberEntity.class));
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando membro não-titular tenta remover outro membro")
    void deveLancarExcecaoQuandoMembroNaoTitularTentaRemoverOutroMembro() {
        // Arrange
        KeycloakUserDto membroNaoTitular = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                false
        );

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(membroNaoTitular));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> familyService.removeFamilyMember(jwt, "maria.santos"));

        assertEquals("Somente o titular pode remover outros membros", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(familyMemberRepository, never()).findByUsername(anyString());
        verify(familyMemberRepository, never()).delete(any(FamilyMemberEntity.class));
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao remover membro quando ele não for encontrado")
    void deveLancarExcecaoAoRemoverMembroQuandoNaoForEncontrado() {
        // Arrange
        KeycloakUserDto holderComFamilia = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                true
        );

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderComFamilia));
        when(familyMemberRepository.findByUsername("usuario.inexistente"))
                .thenReturn(Optional.empty());

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> familyService.removeFamilyMember(jwt, "usuario.inexistente"));

        assertEquals("Membro não encontrado", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(familyMemberRepository, times(1)).findByUsername("usuario.inexistente");
        verify(familyMemberRepository, never()).delete(any(FamilyMemberEntity.class));
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve deletar família com sucesso")
    void deveDeletarFamiliaComSucesso() {
        // Arrange
        KeycloakUserDto holderComFamilia = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                true
        );

        FamilyEntity familyEntity = FamilyEntity.builder()
                .id(1L)
                .holderId("user-id-123")
                .name("Família Silva")
                .build();

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderComFamilia));
        when(familyRepository.findById(1L)).thenReturn(Optional.of(familyEntity));
        doNothing().when(familyMemberRepository).deleteByFamilyId(1L);
        doNothing().when(familyRepository).deleteById(1L);
        doNothing().when(eventPublisher).publish(any(EventType.class), anyString(), any());

        // Act
        familyService.deleteFamily(jwt);

        // Assert
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(familyRepository, times(1)).findById(1L);
        verify(familyMemberRepository, times(1)).deleteByFamilyId(1L);
        verify(familyRepository, times(1)).deleteById(1L);
        verify(eventPublisher, times(1)).publish(EventType.FAMILY_DELETED, "user-id-123", "Família Silva");
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar família quando usuário não pertence a uma")
    void deveLancarExcecaoAoDeletarFamiliaQuandoUsuarioNaoPertence() {
        // Arrange
        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderUserDto));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> familyService.deleteFamily(jwt));

        assertEquals("Usuário não pertence a uma família", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(familyRepository, never()).findById(anyLong());
        verify(familyMemberRepository, never()).deleteByFamilyId(anyLong());
        verify(familyRepository, never()).deleteById(anyLong());
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar família quando usuário não é titular")
    void deveLancarExcecaoAoDeletarFamiliaQuandoUsuarioNaoETitular() {
        // Arrange
        KeycloakUserDto membroNaoTitular = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                false
        );

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(membroNaoTitular));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> familyService.deleteFamily(jwt));

        assertEquals("Somente o titular pode remover a família", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(familyRepository, never()).findById(anyLong());
        verify(familyMemberRepository, never()).deleteByFamilyId(anyLong());
        verify(familyRepository, never()).deleteById(anyLong());
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar família quando ela não for encontrada")
    void deveLancarExcecaoAoDeletarFamiliaQuandoNaoForEncontrada() {
        // Arrange
        KeycloakUserDto holderComFamilia = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                true
        );

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderComFamilia));
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> familyService.deleteFamily(jwt));

        assertEquals("Família não encontrada", exception.getMessage());
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
        verify(familyRepository, times(1)).findById(1L);
        verify(familyMemberRepository, never()).deleteByFamilyId(anyLong());
        verify(familyRepository, never()).deleteById(anyLong());
        verify(eventPublisher, never()).publish(any(EventType.class), anyString(), any());
    }

    @Test
    @DisplayName("Deve buscar usuário usando realm e username do JWT")
    void deveBuscarUsuarioComRealmEUsername() {
        // Arrange
        KeycloakUserDto holderComFamilia = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                true
        );

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderComFamilia));
        when(keycloakUserDao.findByFamilyId(1L)).thenReturn(List.of(holderComFamilia));

        // Act
        familyService.getFamily(jwt);

        // Assert
        verify(keycloakConfig, times(1)).getRealm();
        verify(keycloakUserDao, times(1)).findByRealmAndUsername("development", "joao.silva");
    }

    @Test
    @DisplayName("Deve marcar membro como deletável corretamente quando é o titular")
    void deveMarcaMembroComoDeletevelQuandoETitular() {
        // Arrange
        KeycloakUserDto holderComFamilia = new KeycloakUserDto(
                "user-id-123",
                "joao.silva",
                "João",
                "Silva",
                "joao@email.com",
                true,
                1L,
                "Família Silva",
                true
        );

        List<KeycloakUserDto> membros = List.of(
                holderComFamilia,
                new KeycloakUserDto("user-id-456", "maria.santos", "Maria", "Santos", "maria@email.com", true, 1L, "Família Silva", false)
        );

        when(keycloakUserDao.findByRealmAndUsername("development", "joao.silva"))
                .thenReturn(Optional.of(holderComFamilia));
        when(keycloakUserDao.findByFamilyId(1L)).thenReturn(membros);

        // Act
        FamilyResponse response = familyService.getFamily(jwt);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.membros().size());

        // Titular não pode ser deletado
        FamilyMemberResponse titular = response.membros().stream()
                .filter(m -> m.username().equals("joao.silva"))
                .findFirst()
                .orElseThrow();
        assertFalse(titular.ehDeletavel(), "Titular não deve ser deletável");

        // Membro pode ser deletado
        FamilyMemberResponse membro = response.membros().stream()
                .filter(m -> m.username().equals("maria.santos"))
                .findFirst()
                .orElseThrow();
        assertTrue(membro.ehDeletavel(), "Membro não titular deve ser deletável");
    }
}

