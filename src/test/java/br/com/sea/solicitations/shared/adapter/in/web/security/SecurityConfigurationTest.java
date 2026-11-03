package br.com.sea.solicitations.shared.adapter.in.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigurationTest.PublicEndpoints.class)
@Import({
        SecurityConfiguration.class,
        SecurityErrorHandler.class,
        SecurityConfigurationTest.PublicEndpoints.class
})
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void allowsClientRegistrationWithoutAuthenticationOrCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/register"))
                .andExpect(status().isNoContent());
    }

    @Test
    void allowsLoginWithoutAuthenticationOrCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/login"))
                .andExpect(status().isNoContent());
    }

    @Test
    void allowsHealthEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isNoContent());
    }

    @Test
    void allowsPrometheusMetricsOnlyToAdministrators() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/actuator/prometheus")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/actuator/prometheus")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/actuator/prometheus")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsStandardizedUnauthorizedResponseForProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/protected"));
    }

    @Test
    void allowsProtectedEndpointWithJwtAuthentication() throws Exception {
        mockMvc.perform(get("/protected").with(jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void allowsAdminEndpointOnlyToAdmin() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsAnalystEndpointOnlyToAnalyst() throws Exception {
        mockMvc.perform(get("/analyst/solicitations/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/analyst/solicitations/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsSolicitationEndpointOnlyToClient() throws Exception {
        mockMvc.perform(post("/solicitations/1/submit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/solicitations/1/submit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void convertsRoleClaimToSpringSecurityAuthority() {
        Instant issuedAt = Instant.now();
        Jwt token = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("user-id")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(3600))
                .claim("role", "ANALYST")
                .build();

        var authentication = new SecurityConfiguration()
                .jwtAuthenticationConverter()
                .convert(token);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ANALYST");
    }

    @Test
    void allowsSwaggerAndOpenApiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isNoContent());
    }

    @RestController
    public static class PublicEndpoints {

        @PostMapping("/auth/register")
        public ResponseEntity<Void> register() {
            return ResponseEntity.noContent().build();
        }

        @PostMapping("/auth/login")
        public ResponseEntity<Void> login() {
            return ResponseEntity.noContent().build();
        }

        @PostMapping("/admin/users")
        public ResponseEntity<Void> createInternalUser() {
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/analyst/solicitations/1")
        public ResponseEntity<Void> readSolicitationAsAnalyst() {
            return ResponseEntity.noContent().build();
        }

        @PostMapping("/solicitations/1/submit")
        public ResponseEntity<Void> submitSolicitation() {
            return ResponseEntity.noContent().build();
        }

        @GetMapping({
                "/actuator/health",
                "/actuator/health/liveness",
                "/actuator/prometheus",
                "/v3/api-docs",
                "/swagger-ui/index.html",
                "/swagger-ui.html"
        })
        public ResponseEntity<Void> publicGet() {
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/protected")
        public ResponseEntity<Void> protectedEndpoint() {
            return ResponseEntity.noContent().build();
        }
    }
}
