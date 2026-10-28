package br.com.sea.solicitations.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 40)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, updatable = false, length = 30)
    private AuditResourceType resourceType;

    @Column(name = "resource_id", updatable = false)
    private UUID resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private AuditOutcome outcome;

    @Column(name = "request_id", updatable = false, length = 100)
    private String requestId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditLog() {
    }

    public AuditLog(
            UUID actorId,
            AuditAction action,
            AuditResourceType resourceType,
            UUID resourceId,
            AuditOutcome outcome,
            String requestId,
            Map<String, Object> metadata,
            Instant occurredAt
    ) {
        this.actorId = actorId;
        this.action = Objects.requireNonNull(action, "Audit action is required");
        this.resourceType = Objects.requireNonNull(resourceType, "Audit resource type is required");
        this.resourceId = resourceId;
        this.outcome = Objects.requireNonNull(outcome, "Audit outcome is required");
        this.requestId = optionalText(requestId, 100);
        this.metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        this.occurredAt = Objects.requireNonNull(occurredAt, "Audit time is required");
    }

    private static String optionalText(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException("Request id must have at most " + maximumLength + " characters");
        }
        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditResourceType getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public String getRequestId() {
        return requestId;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
