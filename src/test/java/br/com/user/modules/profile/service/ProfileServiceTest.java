package br.com.user.modules.profile.service;

import br.com.user.modules.profile.ProfileEntity;
import br.com.user.modules.profile.ProfileEventPublisher;
import br.com.user.modules.profile.ProfileMapper;
import br.com.user.modules.profile.ProfileRepository;
import br.com.user.modules.profile.ProfileService;
import br.com.user.modules.profile.dto.PasswordChangeRequest;
import br.com.user.modules.profile.dto.ProfileResponse;
import br.com.user.modules.profile.dto.ProfileUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileServiceTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private ProfileMapper profileMapper;
    @Mock private ProfileEventPublisher eventPublisher;
    @Mock private RestClient keycloakAdminRestClient;

    // POST chain (admin token + ROPC)
    @Mock private RestClient.RequestBodyUriSpec postUriSpec;
    @Mock private RestClient.RequestBodySpec postBodySpec;
    @Mock private RestClient.ResponseSpec postResponseSpec;

    // GET chain (findKeycloakUserId)
    @Mock private RestClient.RequestHeadersUriSpec<?> getUriSpec;
    @Mock private RestClient.RequestHeadersSpec<?> getHeadersSpec;
    @Mock private RestClient.ResponseSpec getResponseSpec;

    // PUT chain (sync user + reset password)
    @Mock private RestClient.RequestBodyUriSpec putUriSpec;
    @Mock private RestClient.RequestBodySpec putBodySpec;
    @Mock private RestClient.ResponseSpec putResponseSpec;

    @InjectMocks
    private ProfileService profileService;

    private Jwt jwt;
    private ProfileEntity profile;
    private ProfileResponse profileResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(profileService, "realm", "development");
        ReflectionTestUtils.setField(profileService, "clientId", "user-api");
        ReflectionTestUtils.setField(profileService, "clientSecret", "secret");
        ReflectionTestUtils.setField(profileService, "issuerUri", "http://localhost:9999/realms/development");

        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("keycloak-uuid-123");
        when(jwt.getClaimAsString("preferred_username")).thenReturn("joao.silva");
        when(jwt.getClaimAsString("name")).thenReturn("JoÃ£o Silva");
        when(jwt.getClaimAsString("email")).thenReturn("joao@email.com");

        profile = ProfileEntity.builder()
                .id(1L).username("joao.silva").nomeCompleto("João Silva")
                .email("joao@email.com").telefone("11999999999")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        profileResponse = ProfileResponse.builder()
                .cpf(null).nomeCompleto("João Silva")
                .email("joao@email.com").telefone("11999999999").build();

        // POST chain: used by obtainAdminToken() and validateCurrentPassword()
        when(keycloakAdminRestClient.post()).thenReturn(postUriSpec);
        when(postUriSpec.uri(anyString())).thenReturn(postBodySpec);
        when(postBodySpec.contentType(any())).thenReturn(postBodySpec);
        when(postBodySpec.body(anyString())).thenReturn(postBodySpec);
        when(postBodySpec.retrieve()).thenReturn(postResponseSpec);
        when(postResponseSpec.body(Map.class)).thenReturn(Map.of("access_token", "admin-token"));
        // postResponseSpec.toBodilessEntity() returns null by default → ROPC ok

        // GET chain: used by findKeycloakUserId()
        when(keycloakAdminRestClient.get()).thenAnswer(inv -> getUriSpec);
        when(getUriSpec.uri(any(URI.class))).thenAnswer(inv -> getHeadersSpec);
        when(getHeadersSpec.header(anyString(), anyString())).thenAnswer(inv -> getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.body(List.class)).thenReturn(List.of(Map.of("id", "keycloak-uuid-123")));

        // PUT chain: used by syncKeycloak() and resetPasswordInKeycloak()
        when(keycloakAdminRestClient.put()).thenReturn(putUriSpec);
        when(putUriSpec.uri(any(URI.class))).thenReturn(putBodySpec);
        when(putBodySpec.header(anyString(), anyString())).thenReturn(putBodySpec);
        when(putBodySpec.contentType(any())).thenReturn(putBodySpec);
        when(putBodySpec.body((Object) any())).thenReturn(putBodySpec);
        when(putBodySpec.retrieve()).thenReturn(putResponseSpec);
    }

    @Test
    void getProfileShouldReturnExistingProfile() {
        when(profileRepository.findByUsername("joao.silva")).thenReturn(Optional.of(profile));
        when(profileMapper.toResponse(profile)).thenReturn(profileResponse);

        ProfileResponse result = profileService.getProfile(jwt);

        assertThat(result).isEqualTo(profileResponse);
        verify(profileRepository).findByUsername("joao.silva");
        verify(profileMapper).toResponse(profile);
        verifyNoMoreInteractions(profileRepository);
    }

    @Test
    void getProfileShouldAutoProvisionFromJwtClaims() {
        when(profileRepository.findByUsername("joao.silva")).thenReturn(Optional.empty());
        when(profileRepository.save(any(ProfileEntity.class))).thenReturn(profile);
        when(profileMapper.toResponse(profile)).thenReturn(profileResponse);

        ProfileResponse result = profileService.getProfile(jwt);

        assertThat(result).isEqualTo(profileResponse);
        verify(profileRepository).save(any(ProfileEntity.class));
    }

    @Test
    void updateProfileShouldSaveAndPublishEvent() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setNomeCompleto("João Atualizado");
        request.setCpf("123.456.789-00");
        request.setEmail("joao.novo@email.com");
        request.setTelefone("11888888888");

        when(profileRepository.findByUsername("joao.silva")).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(ProfileEntity.class))).thenReturn(profile);
        when(profileMapper.toResponse(profile)).thenReturn(profileResponse);

        ProfileResponse result = profileService.updateProfile(jwt, request);

        assertThat(result).isEqualTo(profileResponse);
        verify(profileRepository).save(profile);
        verify(eventPublisher).publishProfileUpdated(1L, "joao.silva");
    }

    @Test
    void updateProfileShouldSyncNameAndEmailToKeycloak() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setNomeCompleto("JoÃ£o Atualizado");
        request.setCpf("123.456.789-00");
        request.setEmail("joao.novo@email.com");
        request.setTelefone("11888888888");

        when(profileRepository.findByUsername("joao.silva")).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(ProfileEntity.class))).thenReturn(profile);
        when(profileMapper.toResponse(profile)).thenReturn(profileResponse);

        profileService.updateProfile(jwt, request);

        verify(keycloakAdminRestClient, atLeastOnce()).post();
        verify(keycloakAdminRestClient, atLeastOnce()).put();
    }

    @Test
    void updateProfileShouldRollbackWhenKeycloakSyncFails() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setNomeCompleto("João Atualizado");
        request.setEmail("joao.novo@email.com");
        request.setTelefone("11888888888");

        when(profileRepository.findByUsername("joao.silva")).thenReturn(Optional.of(profile));
        when(putResponseSpec.toBodilessEntity()).thenThrow(new RuntimeException("Keycloak unavailable"));

        assertThatThrownBy(() -> profileService.updateProfile(jwt, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));

        verify(profileRepository, never()).save(any());
        verify(eventPublisher, never()).publishProfileUpdated(any(), any());
    }

    @Test
    void updateProfileShouldThrowWhenProfileNotFound() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setNomeCompleto("JoÃ£o Atualizado");
        request.setEmail("joao@email.com");

        when(profileRepository.findByUsername("joao.silva")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.updateProfile(jwt, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(eventPublisher, never()).publishProfileUpdated(any(), any());
    }

    @Test
    void changePasswordShouldCallKeycloakAndPublishEvent() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setSenhaAtual("senhaAntiga");
        request.setNovaSenha("novaSenha123");
        request.setConfirmarNovaSenha("novaSenha123");

        when(profileRepository.findByUsername("joao.silva")).thenReturn(Optional.of(profile));

        profileService.changePassword(jwt, request);

        verify(eventPublisher).publishProfilePasswordChanged(1L, "joao.silva");
    }

    @Test
    void changePasswordShouldThrowWhenCurrentPasswordInvalid() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setSenhaAtual("senhaErrada");
        request.setNovaSenha("novaSenha123");
        request.setConfirmarNovaSenha("novaSenha123");

        when(postResponseSpec.toBodilessEntity()).thenThrow(new RuntimeException("401 Unauthorized"));

        assertThatThrownBy(() -> profileService.changePassword(jwt, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(eventPublisher, never()).publishProfilePasswordChanged(any(), any());
    }

    @Test
    void changePasswordShouldThrowWhenPasswordsDoNotMatch() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setSenhaAtual("senhaAntiga");
        request.setNovaSenha("novaSenha123");
        request.setConfirmarNovaSenha("outraSenha456");

        assertThatThrownBy(() -> profileService.changePassword(jwt, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(keycloakAdminRestClient);
        verify(eventPublisher, never()).publishProfilePasswordChanged(any(), any());
    }
}
