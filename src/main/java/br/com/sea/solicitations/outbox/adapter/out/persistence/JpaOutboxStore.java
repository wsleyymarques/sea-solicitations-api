package br.com.sea.solicitations.outbox.adapter.out.persistence;

import br.com.sea.solicitations.outbox.application.port.out.OutboxStore;
import br.com.sea.solicitations.outbox.domain.OutboxEvent;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Repository
public class JpaOutboxStore implements OutboxStore {

    private final SpringDataOutboxEventJpaRepository jpa;

    public JpaOutboxStore(SpringDataOutboxEventJpaRepository jpa) {
        this.jpa = Objects.requireNonNull(jpa, "Outbox JPA repository is required");
    }

    @Override
    public void save(OutboxEvent event) {
        jpa.save(Objects.requireNonNull(event, "Outbox event is required"));
    }

    @Override
    public List<OutboxEvent> claimReady(Instant now, int limit) {
        Objects.requireNonNull(now, "Claim time is required");
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Claim limit must be between 1 and 100");
        }
        return jpa.claimReady(now, limit);
    }
}
