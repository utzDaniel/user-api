package br.com.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;

@Component
public class KeycloakAdminClient {

    private final RestClient restClient;
    private final KeycloakConfig keycloakConfig;
    private final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    public KeycloakAdminClient(RestClient keycloakAdminRestClient, KeycloakConfig keycloakConfig) {
        this.restClient = keycloakAdminRestClient;
        this.keycloakConfig = keycloakConfig;
    }

    public String obtainAdminToken() {
        try {
            Map<?, ?> response = restClient.post()
                    .uri(keycloakConfig.getAdminTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(keycloakConfig.getAdminTokenBody())
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("access_token")) {
                log.warn("Erro ao obter 'access_token' do admin do Keycloak");
                throw ApiException.badGateway("Erro interno contate o administrador do Sistema");
            }
            return (String) response.get("access_token");

        } catch (HttpClientErrorException e) {
            log.warn("'clientId' ou 'clientSecret' do admin inválido no Keycloak");
            throw ApiException.badGateway("Erro interno contate o administrador do Sistema");
        } catch (Exception e) {
            log.warn("Não foi possível obter token de admin do Keycloak", e);
            throw ApiException.badGateway("Erro interno contate o administrador do Sistema");
        }
    }

    public void validatePassword(String username, String password) {
        try {
            restClient.post()
                    .uri(keycloakConfig.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(keycloakConfig.getPasswordGrantBody(username, password))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            log.debug("Senha inválida para o usuário: {}", username);
            throw ApiException.badRequest(java.util.List.of(
                    new Violacao("senhaAtual", "Senha atual inválida")));
        } catch (Exception e) {
            log.error("Erro ao validar senha atual no Keycloak para usuário: {}", username, e);
            throw ApiException.badGateway("Erro ao validar senha atual");
        }
    }

    public void updateUser(String userId, String firstName, String lastName, String email, boolean emailVerified) {
        String adminToken = obtainAdminToken();
        Map<String, Object> body = Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "email", email,
                "emailVerified", emailVerified
        );

        try {
            restClient.put()
                    .uri(URI.create(keycloakConfig.getUserAdminUrl(userId)))
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Usuário {} atualizado no Keycloak com sucesso", userId);
        } catch (Exception e) {
            log.error("Erro ao atualizar dados do usuário {} no Keycloak", userId, e);
            throw ApiException.badGateway("Erro ao atualizar dados");
        }
    }

    public void resetPassword(String userId, String newPassword) {
        String adminToken = obtainAdminToken();
        Map<String, Object> body = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", false
        );

        try {
            restClient.put()
                    .uri(URI.create(keycloakConfig.getResetPasswordUrl(userId)))
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Senha do usuário {} resetada com sucesso", userId);
        } catch (Exception e) {
            log.error("Erro ao resetar senha do usuário {} no Keycloak", userId, e);
            throw ApiException.badGateway("Erro ao trocar de senha");
        }
    }
}

