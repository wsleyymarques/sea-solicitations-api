package br.com.sea.solicitations.audit.adapter.in.aop;

import br.com.sea.solicitations.audit.application.annotation.Audited;
import br.com.sea.solicitations.audit.application.port.out.AuditLogWriter;
import br.com.sea.solicitations.audit.domain.AuditLog;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuditAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogWriter writer;
    private final AuditLogFactory factory;
    private final Counter writeFailures;

    public AuditAspect(AuditLogWriter writer, AuditLogFactory factory, MeterRegistry registry) {
        this.writer = Objects.requireNonNull(writer, "Audit writer is required");
        this.factory = Objects.requireNonNull(factory, "Audit log factory is required");
        this.writeFailures = Counter.builder("sea.audit.write.failures")
                .description("Audit writes that could not be persisted")
                .register(Objects.requireNonNull(registry, "Meter registry is required"));
    }

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object command = joinPoint.getArgs().length == 0 ? null : joinPoint.getArgs()[0];
        try {
            Object result = joinPoint.proceed();
            writeSafely(audited, command, result, null);
            return result;
        } catch (Throwable failure) {
            writeSafely(audited, command, null, failure);
            throw failure;
        }
    }

    private void writeSafely(Audited audited, Object command, Object result, Throwable failure) {
        try {
            List<AuditLog> logs = factory.create(audited.value(), command, result, failure);
            writer.saveAll(logs);
        } catch (RuntimeException auditFailure) {
            writeFailures.increment();
            LOGGER.error("Could not persist audit log for {}", audited.value(), auditFailure);
        }
    }
}
