package br.com.user.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
@DisplayName("Testes unitários de SecurityConfig")
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private JwtDecoder jwtDecoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Deve ter SecurityFilterChain configurado")
    void deveTermSecurityFilterChainConfigurado() {
        assertNotNull(securityFilterChain);
    }

    @Test
    @DisplayName("Deve ter JwtDecoder configurado")
    void deveTermJwtDecoderConfigurado() {
        assertNotNull(jwtDecoder);
    }

    @Test
    @DisplayName("Deve permitir acesso público ao /actuator/health sem autenticação")
    void devePermitirAcessoHealthSemToken() throws Exception {
        // Health pode retornar 503 se serviços não estiverem disponíveis, mas não deve retornar 401 (não autorizado)
        int status = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getStatus();
        // Verifica que não é 401 - pode ser 200 ou 503
        assertTrue(status != 401, "Endpoint /actuator/health não deve exigir autenticação");
    }

    @Test
    @DisplayName("Deve retornar 401 ao acessar endpoint protegido sem token")
    void deveRetornar401AoAcessarEndpointProtegidoSemToken() throws Exception {
        mockMvc.perform(get("/api/v1/user/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Não autorizado"));
    }

    @Test
    @DisplayName("Deve permitir OPTIONS sem autenticação")
    void devePermitirOptionsSemAutenticacao() throws Exception {
        // OPTIONS é permitido mesmo que o endpoint não exista - faz parte da configuração CORS
        int status = mockMvc.perform(options("/api/v1/user/test"))
                .andReturn().getResponse().getStatus();
        // Pode retornar 200 (OK) ou 404 (endpoint não existe), mas não deve retornar 401 (não autorizado)
        assertTrue(status != 401, "Requisição OPTIONS não deve exigir autenticação");
    }

    @Test
    @DisplayName("Deve ter CORS configurado para localhost:4200")
    void deveTermCorsConfiguradoParaLocalhost() throws Exception {
        mockMvc.perform(options("/api/v1/user/test")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    @DisplayName("Deve bloquear CORS para origens não permitidas")
    void deveBloqueiarCorsParaOrigensNaoPermitidas() throws Exception {
        mockMvc.perform(get("/api/v1/user/me")
                        .header("Origin", "http://evil.com"))
                .andExpect(status().is4xxClientError()) // Pode ser 401 ou 403
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
