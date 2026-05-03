package br.com.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class KeycloakAdminConfig {

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Bean
    public RestClient keycloakAdminRestClient() {
        return RestClient.builder()
                .baseUrl(serverUrl)
                .build();
    }
}
