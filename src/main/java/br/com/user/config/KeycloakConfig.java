package br.com.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Getter
@Setter
public class KeycloakConfig {

    private String url;
    private String realm;
    private String clientId;
    private String clientSecret;


    @Bean
    public RestClient keycloakAdminRestClient() {
        return RestClient.builder()
                .baseUrl(url)
                .build();
    }

    public String getJwkSetUri() {
        return String.format("%s/realms/%s/protocol/openid-connect/certs", url, realm);
    }

    public String getAdminTokenUrl() {
        return String.format("%s/realms/%s/protocol/openid-connect/token", url, realm);
    }

    public String getUserAdminUrl(String userId) {
        return String.format("%s/admin/realms/%s/users/%s", url, realm, userId);
    }

    public String getResetPasswordUrl(String userId) {
        return String.format("%s/admin/realms/%s/users/%s/reset-password", url, realm, userId);
    }

    public String getTokenUrl() {
        return String.format("%s/realms/%s/protocol/openid-connect/token", url, realm);
    }

    public String getAdminTokenBody() {
        return String.format("grant_type=client_credentials&client_id=%s&client_secret=%s",
                clientId, clientSecret);
    }

    public String getPasswordGrantBody(String username, String password) {
        return String.format("grant_type=password&client_id=%s&client_secret=%s&username=%s&password=%s",
                clientId, clientSecret, username, password);
    }
}

