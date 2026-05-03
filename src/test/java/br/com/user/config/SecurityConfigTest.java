package br.com.user.config;

import br.com.user.modules.family.FamilyService;
import br.com.user.modules.profile.ProfileRepository;
import br.com.user.modules.profile.ProfileService;
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

// Testes migrados de scope-based (user.read/write) para role-based (ROLE_ADMIN/ROLE_USER)

@WebMvcTest
@Import(SecurityConfig.class)
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://mock-keycloak/realms/development")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private FamilyService familyService;

    @MockitoBean
    private ProfileRepository profileRepository;

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

    // ── /api/v1/users/** ─────────────────────────────────────────────────────

    @Test
    void getUsersShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void getUsersShouldReturn403WithTokenButWithoutRole() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .with(jwt()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void getUsersShouldReturn403WithUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void getUsersShouldBeAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
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
    void postUsersShouldReturn403WithUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType("application/json")
                .content("{}")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void postUsersShouldBeAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType("application/json")
                .content("{}")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
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
    void putUserShouldReturn403WithUserRole() throws Exception {
        mockMvc.perform(put("/api/v1/users/1")
                .contentType("application/json")
                .content("{}")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void putUserShouldBeAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(put("/api/v1/users/1")
                .contentType("application/json")
                .content("{}")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void deleteUserShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUserShouldReturn403WithUserRole() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteUserShouldBeAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void getUserRolesShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/roles"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserRolesShouldReturn403WithUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/roles")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void getUserRolesShouldBeAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/roles")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
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
    void putUserRolesShouldReturn403WithUserRole() throws Exception {
        mockMvc.perform(put("/api/v1/users/1/roles")
                .contentType("application/json")
                .content("[]")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void putUserRolesShouldBeAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(put("/api/v1/users/1/roles")
                .contentType("application/json")
                .content("[]")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    // ── /api/v1/profile/** ───────────────────────────────────────────────────

    @Test
    void getProfileShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/profile"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfileShouldReturn403WithoutRole() throws Exception {
        mockMvc.perform(get("/api/v1/profile")
                .with(jwt()))
            .andExpect(status().isForbidden());
    }

    @Test
    void getProfileShouldBeAccessibleWithUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/profile")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void getProfileShouldBeAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/profile")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }
}
