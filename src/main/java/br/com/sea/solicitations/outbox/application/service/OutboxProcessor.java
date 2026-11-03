package br.com.sea.solicitations.outbox.application.service;

import br.com.sea.solicitations.outbox.application.port.out.OutboxStore;
import br.com.sea.solicitations.outbox.application.port.out.OutboxMetrics;
import br.com.sea.solicitations.outbox.domain.OutboxEvent;
import br.com.sea.solicitations.outbox.domain.OutboxEventType;
import br.com.sea.solicitations.solicitation.application.exception.SearchUnavailableException;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationSearchIndex;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class OutboxProcessor {

    private final OutboxStore store;
    private final SolicitationRepository solicitations;
    private final SolicitationSearchIndex index;
    private final Clock clock;
    private final OutboxMetrics metrics;

    public OutboxProcessor(
            OutboxStore store,
            SolicitationRepository solicitations,
            SolicitationSearchIndex index,
            Clock clock,
            OutboxMetrics metrics
    ) {
        this.store = Objects.requireNonNull(store, "Outbox store is required");
        this.solicitations = Objects.requireNonNull(solicitations, "Solicitation repository is required");
        this.index = Objects.requireNonNull(index, "Search index is required");
        this.clock = Objects.requireNonNull(clock, "Clock is required");
        this.metrics = Objects.requireNonNull(metrics, "Outbox metrics are required");
    }

    @Transactional
    public void processBatch() {
        Instant now = clock.instant();
        for (OutboxEvent event : store.claimReady(now, 20)) {
            if (event.getEventType() != OutboxEventType.SOLICITATION_INDEX_REQUESTED) {
                throw new IllegalStateException("Unsupported outbox event type: " + event.getEventType());
            }
            Optional<Solicitation> solicitation = solicitations.findById(event.getAggregateId());
            if (solicitation.isEmpty()) {
                event.markProcessed(clock.instant());
                store.save(event);
                metrics.processed();
                continue;
            }
            boolean retry = false;
            try {
                index.index(event.getAggregateId(), solicitation.get());
                event.markProcessed(clock.instant());
            } catch (SearchUnavailableException exception) {
                event.registerFailure(exception.getMessage(), clock.instant());
                retry = true;
            }
            store.save(event);
            if (retry) {
                metrics.retry();
            } else {
                metrics.processed();
            }
        }
    }
}
