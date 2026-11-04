package br.com.sea.solicitations.audit.adapter.in.aop;

import br.com.sea.solicitations.audit.domain.AuditAction;
import br.com.sea.solicitations.audit.domain.AuditLog;
import br.com.sea.solicitations.audit.domain.AuditOutcome;
import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.DecisionOutcome;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserCommand;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserResult;
import br.com.sea.solicitations.user.domain.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogFactoryTest {

    private static final Instant NOW = Instant.parse("2026-11-02T15:00:00Z");
    private final AuditLogFactory factory = new AuditLogFactory(Clock.fixed(NOW, ZoneOffset.UTC));

    @AfterEach
    void cleanContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void usesAuthenticatedActorAndRecordsRejectionWithoutComment() {
        UUID analyst = UUID.randomUUID();
        UUID authenticated = UUID.randomUUID();
        UUID solicitation = UUID.randomUUID();
        Jwt jwt = new Jwt("token-secret", NOW, NOW.plusSeconds(60),
                Map.of("alg", "none"), Map.of("sub", authenticated.toString()));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_ANALYST"))));
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
        assertThat(jwt.getSubject()).isEqualTo(authenticated.toString());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "req-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        List<AuditLog> logs = factory.create(AuditAction.SOLICITATION_APPROVED,
                new DecideSolicitationCommand(solicitation, analyst, Set.of(FederativeUnit.SP),
                        DecisionOutcome.REJECT, "private decision comment"),
                null, new IllegalStateException("private failure message"));

        assertThat(logs).hasSize(1);
        AuditLog log = logs.getFirst();
        assertThat(log.getAction()).isEqualTo(AuditAction.SOLICITATION_REJECTED);
        assertThat(log.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(log.getActorId()).isEqualTo(authenticated);
        assertThat(log.getResourceId()).isEqualTo(solicitation);
        assertThat(log.getRequestId()).isEqualTo("req-123");
        assertThat(log.getOccurredAt()).isEqualTo(NOW);
        assertThat(log.getMetadata()).containsEntry("outcome", "REJECT")
                .containsEntry("errorType", "IllegalStateException")
                .doesNotContainKeys("comment", "message", "token");
        assertThat(log.getMetadata().toString()).doesNotContain("private");
    }

    @Test
    void recordsAnalystCreationAndCoverageWithSameRequestId() {
        UUID userId = UUID.randomUUID();
        CreateInternalUserCommand command = new CreateInternalUserCommand(
                "Name", "name@example.com", "secret-password", UserRole.ANALYST,
                Set.of(FederativeUnit.SP, FederativeUnit.RJ));
        CreateInternalUserResult result = new CreateInternalUserResult(userId, "Name",
                "name@example.com", UserRole.ANALYST, Set.of(FederativeUnit.SP, FederativeUnit.RJ));

        List<AuditLog> logs = factory.create(AuditAction.USER_CREATED, command, result, null);

        assertThat(logs).extracting(AuditLog::getAction)
                .containsExactly(AuditAction.USER_CREATED, AuditAction.ANALYST_STATES_ASSIGNED);
        assertThat(logs).allSatisfy(log -> {
            assertThat(log.getResourceId()).isEqualTo(userId);
            assertThat(log.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
            assertThat(log.getRequestId()).isNotBlank();
            assertThat(log.getMetadata().toString()).doesNotContain("secret-password", "name@example.com");
        });
        assertThat(logs.get(0).getRequestId()).isEqualTo(logs.get(1).getRequestId());
        assertThat(logs.get(1).getMetadata()).containsEntry("coveredStates", List.of("RJ", "SP"));
    }

    @Test
    void ignoresUnsafeRequestIdAndDoesNotCreateCoverageLogOnFailure() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "bad\nrequest-id");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        CreateInternalUserCommand command = new CreateInternalUserCommand(
                "Name", "name@example.com", "secret-password", UserRole.ANALYST,
                Set.of(FederativeUnit.SP));

        List<AuditLog> logs = factory.create(AuditAction.USER_CREATED, command,
                null, new IllegalArgumentException("secret-password"));

        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(logs.getFirst().getRequestId()).matches("[0-9a-f-]{36}");
        assertThat(logs.getFirst().getMetadata().toString()).doesNotContain("secret-password");
    }

    @Test
    void fallsBackToCommandActorWhenJwtHasNoSubject() {
        UUID analyst = UUID.randomUUID();
        Jwt jwt = new Jwt("token-secret", NOW, NOW.plusSeconds(60),
                Map.of("alg", "none"), Map.of("scope", "analyst"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_ANALYST")), "fallback-name"));

        AuditLog log = factory.create(AuditAction.SOLICITATION_APPROVED,
                new DecideSolicitationCommand(UUID.randomUUID(), analyst,
                        Set.of(FederativeUnit.SP), DecisionOutcome.APPROVE, "private comment"),
                null, new IllegalStateException("failed")).getFirst();

        assertThat(log.getActorId()).isEqualTo(analyst);
    }
}
