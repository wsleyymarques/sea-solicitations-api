package br.com.sea.solicitations.audit.adapter.out.persistence;

import br.com.sea.solicitations.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataAuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
