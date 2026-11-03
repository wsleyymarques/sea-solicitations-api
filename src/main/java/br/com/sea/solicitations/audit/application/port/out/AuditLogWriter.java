package br.com.sea.solicitations.audit.application.port.out;

import br.com.sea.solicitations.audit.domain.AuditLog;

import java.util.List;

@FunctionalInterface
public interface AuditLogWriter {
    void saveAll(List<AuditLog> logs);
}
