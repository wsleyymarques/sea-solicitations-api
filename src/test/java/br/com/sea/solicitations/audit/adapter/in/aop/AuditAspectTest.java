package br.com.sea.solicitations.audit.adapter.in.aop;

import br.com.sea.solicitations.audit.application.annotation.Audited;
import br.com.sea.solicitations.audit.application.port.out.AuditLogWriter;
import br.com.sea.solicitations.audit.domain.AuditAction;
import br.com.sea.solicitations.audit.domain.AuditLog;
import br.com.sea.solicitations.audit.domain.AuditOutcome;
import br.com.sea.solicitations.user.application.port.in.RegisterClientCommand;
import br.com.sea.solicitations.user.application.port.in.RegisterClientResult;
import br.com.sea.solicitations.user.domain.UserRole;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditAspectTest {

    private final List<AuditLog> written = new ArrayList<>();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final RegisterClientCommand command = new RegisterClientCommand(
            "Test User", "test@example.com", "secret-password");

    @Test
    void recordsSuccessAfterUseCaseReturns() {
        Registration target = proxied(new Registration(), written::addAll);

        RegisterClientResult result = target.register(command);

        assertThat(result.id()).isNotNull();
        assertThat(written).singleElement().satisfies(log -> {
            assertThat(log.getAction()).isEqualTo(AuditAction.USER_CREATED);
            assertThat(log.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
            assertThat(log.getActorId()).isEqualTo(result.id());
            assertThat(log.getResourceId()).isEqualTo(result.id());
            assertThat(log.getMetadata().toString()).doesNotContain("secret-password");
        });
    }

    @Test
    void recordsFailureAndPreservesOriginalException() {
        Registration target = proxied(new Registration(true), written::addAll);

        assertThatThrownBy(() -> target.register(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("business failure");

        assertThat(written).singleElement().satisfies(log -> {
            assertThat(log.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
            assertThat(log.getResourceId()).isNull();
            assertThat(log.getMetadata()).containsEntry("errorType", "IllegalArgumentException");
        });
    }

    @Test
    void writerFailureDoesNotChangeBusinessResultOrException() {
        AuditLogWriter broken = logs -> { throw new IllegalStateException("audit database unavailable"); };

        assertThat(proxied(new Registration(), broken).register(command).id()).isNotNull();
        assertThatThrownBy(() -> proxied(new Registration(true), broken).register(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("business failure");
        assertThat(registry.get("sea.audit.write.failures").counter().count()).isEqualTo(2);
        assertThat(registry.get("sea.audit.write.failures").counter().getId().getTags()).isEmpty();
    }

    private Registration proxied(Registration target, AuditLogWriter writer) {
        AuditLogFactory factory = new AuditLogFactory(Clock.fixed(
                Instant.parse("2026-11-02T15:00:00Z"), ZoneOffset.UTC));
        AspectJProxyFactory proxy = new AspectJProxyFactory(target);
        proxy.addAspect(new AuditAspect(writer, factory, registry));
        return proxy.getProxy();
    }

    static class Registration {
        private final boolean fail;

        Registration() { this(false); }

        Registration(boolean fail) { this.fail = fail; }

        @Audited(AuditAction.USER_CREATED)
        public RegisterClientResult register(RegisterClientCommand command) {
            if (fail) throw new IllegalArgumentException("business failure");
            return new RegisterClientResult(UUID.randomUUID(), command.name(),
                    command.email(), UserRole.CLIENT);
        }
    }
}
