package br.com.sea.solicitations.integration;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.port.out.PostalCodeData;
import br.com.sea.solicitations.solicitation.application.port.out.PostalCodeLookup;
import br.com.sea.solicitations.user.application.port.out.PasswordHasher;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "outbox.polling.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class ApiFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.11-alpine3.24");

    @Autowired private TestRestTemplate http;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordHasher passwordHasher;
    @MockitoBean private PostalCodeLookup postalCodeLookup;

    @Test
    void apiPersistsSolicitationOutboxAndAuditAfterFlywayMigration() {
        String email = "integration-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPassword@123";

        ResponseEntity<JsonNode> registered = post("/auth/register", null,
                Map.of("name", "Integration Client", "email", email, "password", password));
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID clientId = UUID.fromString(registered.getBody().path("id").asText());

        ResponseEntity<JsonNode> loggedIn = post("/auth/login", null,
                Map.of("email", email, "password", password));
        assertThat(loggedIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loggedIn.getBody().path("accessToken").asText();
        assertThat(token).isNotBlank();

        Map<String, Object> solicitation = Map.of(
                "serviceType", "INSTALLATION", "title", "Instalacao residencial",
                "description", "Descricao completa de uma instalacao residencial para o teste de integracao."
        );
        assertThat(post("/solicitations", null, solicitation).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        ResponseEntity<JsonNode> invalid = post("/solicitations", token,
                Map.of("serviceType", "INSTALLATION", "title", "A", "description", "curta"));
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody().path("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(post("/admin/users", token, Map.of()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<JsonNode> created = post("/solicitations", token, solicitation);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID solicitationId = UUID.fromString(created.getBody().path("id").asText());

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM flyway_schema_history
                WHERE success AND version IN ('1', '2')
                """, Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM solicitations WHERE id = ? AND client_id = ?
                """, Integer.class, solicitationId, clientId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_id = ? AND processed_at IS NULL
                """, Integer.class, solicitationId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_logs
                WHERE resource_id = ? AND outcome = 'SUCCESS'
                  AND action IN ('SOLICITATION_CREATED', 'BASIC_DATA_SAVED')
                """, Integer.class, solicitationId)).isEqualTo(2);
    }

    @Test
    void openApiDescribesBearerSecurityPublicAuthAndErrorContract() {
        ResponseEntity<JsonNode> response = http.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode docs = response.getBody();
        JsonNode bearer = docs.path("components").path("securitySchemes").path("bearerAuth");
        assertThat(bearer.path("type").asText()).isEqualTo("http");
        assertThat(bearer.path("scheme").asText()).isEqualTo("bearer");
        JsonNode publicSecurity = docs.path("paths").path("/auth/register")
                .path("post").path("security");
        assertThat(publicSecurity.isArray()).isTrue();
        assertThat(publicSecurity.isEmpty()).isTrue();
        assertThat(docs.path("paths").path("/solicitations").path("post")
                .path("security").toString()).contains("bearerAuth");
        assertThat(docs.path("paths").path("/solicitations").path("post")
                .path("responses").path("400").path("content").path("application/json")
                .path("schema").toString()).contains("ApiErrorResponse");
        assertThat(docs.path("paths").path("/auth/register").path("post")
                .path("requestBody").path("content").path("application/json")
                .path("examples").path("cliente").path("value").path("email").asText())
                .isEqualTo("joao@example.com");

        String[][] protectedOperations = {
                {"/admin/users", "post"},
                {"/solicitations", "post"},
                {"/solicitations/{id}/address", "put"},
                {"/solicitations/{id}/confirmation", "put"},
                {"/solicitations/{id}/submit", "post"},
                {"/analyst/solicitations", "get"},
                {"/analyst/solicitations/search", "get"},
                {"/analyst/solicitations/{id}", "get"},
                {"/analyst/solicitations/{id}/start", "post"},
                {"/analyst/solicitations/{id}/decide", "post"},
                {"/analyst/solicitations/{id}/decision", "post"}
        };
        for (String[] operation : protectedOperations) {
            JsonNode documented = docs.path("paths").path(operation[0]).path(operation[1]);
            assertThat(documented.path("summary").asText())
                    .describedAs(operation[1] + " " + operation[0]).isNotBlank();
            assertThat(documented.path("security").toString())
                    .describedAs(operation[1] + " " + operation[0]).contains("bearerAuth");
            assertThat(documented.path("responses").path("401").isObject())
                    .describedAs(operation[1] + " " + operation[0] + " unauthorized").isTrue();
            assertThat(documented.path("responses").path("403").isObject())
                    .describedAs(operation[1] + " " + operation[0] + " forbidden").isTrue();
        }
    }

    @Test
    void apiCompletesApprovalAndRejectionWithOwnershipCoverageAndTerminalStateChecks() {
        when(postalCodeLookup.find("01001000")).thenReturn(new PostalCodeData(
                "01001000", "Praca da Se", "Se", "Sao Paulo", FederativeUnit.SP
        ));
        String password = "StrongPassword@123";
        String adminEmail = "integration-admin-" + UUID.randomUUID() + "@example.com";
        jdbc.update("""
                INSERT INTO users (id, full_name, email, password_hash, role, active,
                                   created_at, updated_at, version)
                VALUES (?, 'Integration Admin', ?, ?, 'ADMIN', TRUE, now(), now(), 0)
                """, UUID.randomUUID(), adminEmail, passwordHasher.hash(password));
        String adminToken = login(adminEmail, password);

        String clientEmail = "integration-owner-" + UUID.randomUUID() + "@example.com";
        String otherEmail = "integration-other-" + UUID.randomUUID() + "@example.com";
        register(clientEmail, password);
        register(otherEmail, password);
        String clientToken = login(clientEmail, password);
        String otherClientToken = login(otherEmail, password);

        String assignedToken = createAnalyst(adminToken, password, "SP");
        String otherAnalystToken = createAnalyst(adminToken, password, "SP");
        String outsideCoverageToken = createAnalyst(adminToken, password, "RJ");
        assertThat(get("/analyst/solicitations", clientToken).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        UUID approvedId = createDraft(clientToken);
        assertError(post("/solicitations/" + approvedId + "/submit", clientToken, null),
                HttpStatus.CONFLICT, "INCOMPLETE_SOLICITATION");
        assertError(put("/solicitations/" + approvedId + "/address", otherClientToken,
                Map.of("postalCode", "01001-000", "number", "100")),
                HttpStatus.FORBIDDEN, "SOLICITATION_ACCESS_DENIED");
        completeAndSubmit(approvedId, clientToken);

        assertThat(get("/analyst/solicitations?page=0&size=20", assignedToken)
                .getBody().path("content").toString()).contains(approvedId.toString());
        ResponseEntity<JsonNode> submittedDetail = get(
                "/analyst/solicitations/" + approvedId, assignedToken);
        assertThat(submittedDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(submittedDetail.getBody().path("status").asText()).isEqualTo("SUBMITTED");
        assertThat(submittedDetail.getBody().path("address").path("state").asText()).isEqualTo("SP");
        assertError(get("/analyst/solicitations/" + approvedId, outsideCoverageToken),
                HttpStatus.FORBIDDEN, "SOLICITATION_REVIEW_ACCESS_DENIED");
        ResponseEntity<JsonNode> canonicalSearch = get(
                "/analyst/solicitations/search?state=RJ&dateFrom=2026-11-01"
                        + "&dateTo=2026-11-30&sort=CREATED_AT&direction=ASC",
                assignedToken
        );
        assertThat(canonicalSearch.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(canonicalSearch.getBody().path("content").isEmpty()).isTrue();
        assertError(post("/analyst/solicitations/" + approvedId + "/start",
                outsideCoverageToken, null),
                HttpStatus.FORBIDDEN, "SOLICITATION_REVIEW_ACCESS_DENIED");
        ResponseEntity<JsonNode> started = post(
                "/analyst/solicitations/" + approvedId + "/start", assignedToken, null);
        assertThat(started.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(started.getBody().path("status").asText()).isEqualTo("IN_REVIEW");
        assertThat(get("/analyst/solicitations/" + approvedId, assignedToken)
                .getBody().path("status").asText()).isEqualTo("IN_REVIEW");
        assertError(get("/analyst/solicitations/" + approvedId, otherAnalystToken),
                HttpStatus.FORBIDDEN, "SOLICITATION_REVIEW_ACCESS_DENIED");

        String decisionPath = "/analyst/solicitations/" + approvedId + "/decide";
        Map<String, Object> approval = Map.of(
                "outcome", "APPROVED", "comment", "Parecer favoravel para atendimento."
        );
        assertError(post(decisionPath, otherAnalystToken, approval),
                HttpStatus.FORBIDDEN, "SOLICITATION_REVIEW_ACCESS_DENIED");
        assertError(post(decisionPath, assignedToken,
                Map.of("outcome", "APPROVED", "comment", "curto")),
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        ResponseEntity<JsonNode> approved = post(decisionPath, assignedToken, approval);
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approved.getBody().path("status").asText()).isEqualTo("APPROVED");
        assertError(post(decisionPath, assignedToken, approval),
                HttpStatus.CONFLICT, "INVALID_SOLICITATION_STATE");

        UUID rejectedId = createDraft(clientToken);
        completeAndSubmit(rejectedId, clientToken);
        assertThat(post("/analyst/solicitations/" + rejectedId + "/start",
                assignedToken, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<JsonNode> rejected = post(
                "/analyst/solicitations/" + rejectedId + "/decision", assignedToken,
                Map.of("outcome", "REJECTED", "comment", "Parecer desfavoravel apos analise."));
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rejected.getBody().path("status").asText()).isEqualTo("REJECTED");

        assertThat(jdbc.queryForObject(
                "SELECT status FROM solicitations WHERE id = ?", String.class, approvedId))
                .isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject(
                "SELECT status FROM solicitations WHERE id = ?", String.class, rejectedId))
                .isEqualTo("REJECTED");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE aggregate_id IN (?, ?) AND processed_at IS NULL
                """, Integer.class, approvedId, rejectedId)).isEqualTo(12);
    }

    private void register(String email, String password) {
        assertThat(post("/auth/register", null, Map.of(
                "name", "Integration Client", "email", email, "password", password
        )).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private String login(String email, String password) {
        ResponseEntity<JsonNode> response = post("/auth/login", null,
                Map.of("email", email, "password", password));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().path("accessToken").asText();
    }

    private String createAnalyst(String adminToken, String password, String state) {
        String email = "integration-analyst-" + UUID.randomUUID() + "@example.com";
        assertThat(post("/admin/users", adminToken, Map.of(
                "name", "Integration Analyst", "email", email, "password", password,
                "role", "ANALYST", "coveredStates", List.of(state)
        )).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return login(email, password);
    }

    private UUID createDraft(String clientToken) {
        ResponseEntity<JsonNode> response = post("/solicitations", clientToken, Map.of(
                "serviceType", "INSTALLATION", "title", "Instalacao residencial",
                "description", "Descricao completa de uma instalacao residencial para o teste."
        ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(response.getBody().path("id").asText());
    }

    private void completeAndSubmit(UUID id, String clientToken) {
        ResponseEntity<JsonNode> address = put("/solicitations/" + id + "/address",
                clientToken, Map.of("postalCode", "01001-000", "number", "100"));
        assertThat(address.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(address.getBody().path("address").path("state").asText()).isEqualTo("SP");
        ResponseEntity<JsonNode> confirmation = put("/solicitations/" + id + "/confirmation",
                clientToken, Map.of(
                        "priority", "HIGH",
                        "preferredDate", LocalDate.now(ZoneOffset.UTC).plusDays(7).toString(),
                        "estimatedValue", 100.00,
                        "termsAccepted", true
                ));
        assertThat(confirmation.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<JsonNode> submitted = post(
                "/solicitations/" + id + "/submit", clientToken, null);
        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(submitted.getBody().path("status").asText()).isEqualTo("SUBMITTED");
    }

    private static void assertError(
            ResponseEntity<JsonNode> response, HttpStatus status, String code
    ) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody().path("code").asText()).isEqualTo(code);
    }

    private ResponseEntity<JsonNode> post(String path, String token, Map<String, ?> body) {
        return exchange(path, HttpMethod.POST, token, body);
    }

    private ResponseEntity<JsonNode> put(String path, String token, Map<String, ?> body) {
        return exchange(path, HttpMethod.PUT, token, body);
    }

    private ResponseEntity<JsonNode> get(String path, String token) {
        return exchange(path, HttpMethod.GET, token, null);
    }

    private ResponseEntity<JsonNode> exchange(
            String path, HttpMethod method, String token, Map<String, ?> body
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return http.exchange(path, method,
                new HttpEntity<>(body, headers), JsonNode.class);
    }
}
