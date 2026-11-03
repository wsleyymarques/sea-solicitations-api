package br.com.sea.solicitations.audit.adapter.out.persistence;

import br.com.sea.solicitations.audit.application.port.out.AuditLogWriter;
import br.com.sea.solicitations.audit.domain.AuditLog;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Repository
public class JpaAuditLogWriter implements AuditLogWriter {

    private final SpringDataAuditLogRepository repository;

    public JpaAuditLogWriter(SpringDataAuditLogRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Audit repository is required");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAll(List<AuditLog> logs) {
        repository.saveAll(List.copyOf(Objects.requireNonNull(logs, "Audit logs are required")));
    }
}
