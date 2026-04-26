package br.com.user.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(SecurityConfig.class)
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://mock-keycloak/realms/development")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void swaggerUiShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void actuatorHealthShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void actuatorInfoShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/info"))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void actuatorMetricsShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value("Token ausente ou inválido"));
    }

    @Test
    void getUsersShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void getUsersShouldReturn403WithTokenButWithoutScope() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .with(jwt()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void getUsersShouldReturn403WithWrongScope() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .with(jwt().authorities(new SimpleGrantedAuthority("user.write"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void getUsersShouldBeAccessibleWithReadScope() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .with(jwt().authorities(new SimpleGrantedAuthority("user.read"))))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void postUsersShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void postUsersShouldReturn403WithReadScope() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType("application/json")
                .content("{}")
                .with(jwt().authorities(new SimpleGrantedAuthority("user.read"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void postUsersShouldBeAccessibleWithWriteScope() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType("application/json")
                .content("{}")
                .with(jwt().authorities(new SimpleGrantedAuthority("user.write"))))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void putUserShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(put("/api/v1/users/1")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void putUserShouldReturn403WithReadScope() throws Exception {
        mockMvc.perform(put("/api/v1/users/1")
                .contentType("application/json")
                .content("{}")
                .with(jwt().authorities(new SimpleGrantedAuthority("user.read"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void putUserShouldBeAccessibleWithWriteScope() throws Exception {
        mockMvc.perform(put("/api/v1/users/1")
                .contentType("application/json")
                .content("{}")
                .with(jwt().authorities(new SimpleGrantedAuthority("user.write"))))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void deleteUserShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUserShouldReturn403WithReadScope() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("user.read"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteUserShouldBeAccessibleWithWriteScope() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("user.write"))))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }


    @Test
    void getUserRolesShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/roles"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserRolesShouldReturn403WithWriteScope() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/roles")
                .with(jwt().authorities(new SimpleGrantedAuthority("user.write"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void getUserRolesShouldBeAccessibleWithReadScope() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/roles")
                .with(jwt().authorities(new SimpleGrantedAuthority("user.read"))))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void putUserRolesShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(put("/api/v1/users/1/roles")
                .contentType("application/json")
                .content("[]"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void putUserRolesShouldReturn403WithReadScope() throws Exception {
        mockMvc.perform(put("/api/v1/users/1/roles")
                .contentType("application/json")
                .content("[]")
                .with(jwt().authorities(new SimpleGrantedAuthority("user.read"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void putUserRolesShouldBeAccessibleWithWriteScope() throws Exception {
        mockMvc.perform(put("/api/v1/users/1/roles")
                .contentType("application/json")
                .content("[]")
                .with(jwt().authorities(new SimpleGrantedAuthority("user.write"))))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }
}
