package br.com.user.modules.profile;

import br.com.user.modules.profile.dto.PasswordChangeRequest;
import br.com.user.modules.profile.dto.ProfileResponse;
import br.com.user.modules.profile.dto.ProfileUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final ProfileEventPublisher eventPublisher;
    private final RestClient keycloakAdminRestClient;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    public ProfileService(ProfileRepository profileRepository,
                          ProfileMapper profileMapper,
                          ProfileEventPublisher eventPublisher,
                          RestClient keycloakAdminRestClient) {
        this.profileRepository = profileRepository;
        this.profileMapper = profileMapper;
        this.eventPublisher = eventPublisher;
        this.keycloakAdminRestClient = keycloakAdminRestClient;
    }

    public ProfileResponse getProfile(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        ProfileEntity profile = profileRepository.findByUsername(username)
                .orElseGet(() -> autoProvision(jwt));
        return profileMapper.toResponse(profile);
    }

    @Transactional
    public ProfileResponse updateProfile(Jwt jwt, ProfileUpdateRequest request) {
        String username = jwt.getClaimAsString("preferred_username");
        ProfileEntity profile = profileRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil não encontrado"));

        profile.setNomeCompleto(request.getNomeCompleto());
        profile.setCpf(request.getCpf());
        profile.setEmail(request.getEmail());
        profile.setTelefone(request.getTelefone());

        String adminToken = obtainAdminToken();
        String keycloakId = findKeycloakUserId(username, adminToken);
        syncKeycloak(keycloakId, request.getNomeCompleto(), request.getEmail(), adminToken);

        ProfileEntity saved = profileRepository.save(profile);
        eventPublisher.publishProfileUpdated(saved.getId(), username);
        return profileMapper.toResponse(saved);
    }

    @Transactional
    public void changePassword(Jwt jwt, PasswordChangeRequest request) {
        if (!request.getNovaSenha().equals(request.getConfirmarNovaSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "As senhas não conferem");
        }

        String username = jwt.getClaimAsString("preferred_username");

        validateCurrentPassword(username, request.getSenhaAtual());

        String adminToken = obtainAdminToken();
        String keycloakId = findKeycloakUserId(username, adminToken);
        resetPasswordInKeycloak(keycloakId, request.getNovaSenha(), adminToken);

        ProfileEntity profile = profileRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil não encontrado"));

        eventPublisher.publishProfilePasswordChanged(profile.getId(), username);
    }

    private ProfileEntity autoProvision(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        ProfileEntity profile = ProfileEntity.builder()
                .username(username)
                .nomeCompleto(jwt.getClaimAsString("name") != null
                        ? jwt.getClaimAsString("name")
                        : username)
                .email(jwt.getClaimAsString("email") != null
                        ? jwt.getClaimAsString("email")
                        : username + "@provisioned.local")
                .build();
        return profileRepository.save(profile);
    }

    private String[] splitName(String name) {
        if (name == null) return new String[]{"", ""};
        int idx = name.indexOf(' ');
        if (idx < 0) return new String[]{name, ""};
        return new String[]{name.substring(0, idx), name.substring(idx + 1)};
    }

    @SuppressWarnings("unchecked")
    private String findKeycloakUserId(String username, String adminToken) {
        List<Map<String, Object>> users = keycloakAdminRestClient.get()
                .uri(URI.create("/admin/realms/" + realm + "/users?username=" + username + "&exact=true"))
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(List.class);
        if (users == null || users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado no Keycloak");
        }
        return (String) users.get(0).get("id");
    }

    private String obtainAdminToken() {
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";
        Map<?, ?> response = keycloakAdminRestClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret)
                .retrieve()
                .body(Map.class);
        if (response == null || !response.containsKey("access_token")) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível obter token de admin do Keycloak");
        }
        return (String) response.get("access_token");
    }

    private void syncKeycloak(String keycloakId, String nomeCompleto, String email, String adminToken) {
        String[] parts = splitName(nomeCompleto);
        Map<String, Object> body = Map.of(
                "firstName", parts[0],
                "lastName", parts[1],
                "email", email
        );
        try {
            keycloakAdminRestClient.put()
                    .uri(URI.create("/admin/realms/" + realm + "/users/" + keycloakId))
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao sincronizar dados com Keycloak");
        }
    }

    private void validateCurrentPassword(String username, String senhaAtual) {
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";
        try {
            keycloakAdminRestClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=password&client_id=" + clientId + "&client_secret=" + clientSecret
                            + "&username=" + username + "&password=" + senhaAtual)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual inválida");
        }
    }

    private void resetPasswordInKeycloak(String keycloakId, String novaSenha, String adminToken) {
        Map<String, Object> body = Map.of(
                "type", "password",
                "value", novaSenha,
                "temporary", false
        );
        try {
            keycloakAdminRestClient.put()
                    .uri(URI.create("/admin/realms/" + realm + "/users/" + keycloakId + "/reset-password"))
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao redefinir senha no Keycloak");
        }
    }
}
