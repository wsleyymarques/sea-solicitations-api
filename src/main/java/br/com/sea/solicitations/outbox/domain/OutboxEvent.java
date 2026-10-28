package br.com.sea.solicitations.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 60)
    private OutboxEventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = new HashMap<>();

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Version
    private long version;

    protected OutboxEvent() {
    }

    public OutboxEvent(
            UUID aggregateId,
            OutboxEventType eventType,
            Map<String, Object> payload,
            Instant occurredAt
    ) {
        this.aggregateId = Objects.requireNonNull(aggregateId, "Aggregate id is required");
        this.eventType = Objects.requireNonNull(eventType, "Event type is required");
        this.payload = new HashMap<>(Objects.requireNonNull(payload, "Event payload is required"));
        this.occurredAt = Objects.requireNonNull(occurredAt, "Event time is required");
    }

    public void markProcessed(Instant processedAt) {
        if (this.processedAt != null) {
            throw new IllegalStateException("Outbox event has already been processed");
        }
        this.processedAt = Objects.requireNonNull(processedAt, "Processing time is required");
        this.lastError = null;
    }

    public void registerFailure(String errorMessage) {
        if (processedAt != null) {
            throw new IllegalStateException("A processed outbox event cannot fail");
        }
        attempts++;
        lastError = truncateError(errorMessage);
    }

    public boolean isProcessed() {
        return processedAt != null;
    }

    private static String truncateError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Unknown indexing error";
        }
        String normalized = errorMessage.trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }

    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public OutboxEventType getEventType() {
        return eventType;
    }

    public Map<String, Object> getPayload() {
        return Collections.unmodifiableMap(payload);
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public long getVersion() {
        return version;
    }
}
