package br.com.sea.solicitations.audit.adapter.in.aop;

import br.com.sea.solicitations.audit.application.port.out.AuditLogWriter;
import br.com.sea.solicitations.audit.domain.AuditAction;
import br.com.sea.solicitations.audit.domain.AuditLog;
import br.com.sea.solicitations.audit.domain.AuditOutcome;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationUseCase;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.application.service.CreateSolicitationService;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(AuditSpringWiringTest.Config.class)
class AuditSpringWiringTest {

    @Autowired private CreateSolicitationUseCase useCase;
    @Autowired private CapturedAudit captured;

    @Test
    void springProxyAuditsRealUseCaseDeclaredByInterface() {
        UUID clientId = UUID.randomUUID();

        CreateSolicitationResult result = useCase.create(new CreateSolicitationCommand(
                clientId, ServiceType.INSTALLATION, "Install equipment",
                "Description long enough for a valid solicitation"));

        assertThat(captured.logs).extracting(AuditLog::getAction)
                .containsExactly(AuditAction.SOLICITATION_CREATED, AuditAction.BASIC_DATA_SAVED);
        assertThat(captured.logs).allSatisfy(log -> {
            assertThat(log.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
            assertThat(log.getActorId()).isEqualTo(clientId);
            assertThat(log.getResourceId()).isEqualTo(result.id());
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = false)
    static class Config {
        @Bean Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"), ZoneOffset.UTC);
        }
        @Bean CapturedAudit captured() { return new CapturedAudit(); }
        @Bean AuditLogFactory factory(Clock clock) { return new AuditLogFactory(clock); }
        @Bean MeterRegistry registry() { return new SimpleMeterRegistry(); }
        @Bean AuditLogWriter writer(CapturedAudit captured) { return captured.logs::addAll; }
        @Bean AuditAspect aspect(AuditLogWriter writer, AuditLogFactory factory, MeterRegistry registry) {
            return new AuditAspect(writer, factory, registry);
        }
        @Bean SolicitationRepository repository() {
            SolicitationRepository repository = mock(SolicitationRepository.class);
            when(repository.save(any(Solicitation.class))).thenReturn(UUID.randomUUID());
            return repository;
        }
        @Bean CreateSolicitationUseCase useCase(SolicitationRepository repository) {
            return new CreateSolicitationService(repository);
        }
    }

    static class CapturedAudit {
        final List<AuditLog> logs = new ArrayList<>();
    }
}
