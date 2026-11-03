package br.com.sea.solicitations.outbox.adapter.out.metrics;

import br.com.sea.solicitations.outbox.adapter.out.persistence.SpringDataOutboxEventJpaRepository;
import br.com.sea.solicitations.outbox.application.port.out.OutboxMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MicrometerOutboxMetrics implements OutboxMetrics {

    private final Counter processed;
    private final Counter retries;

    public MicrometerOutboxMetrics(
            MeterRegistry registry,
            SpringDataOutboxEventJpaRepository repository
    ) {
        Objects.requireNonNull(registry, "Meter registry is required");
        Objects.requireNonNull(repository, "Outbox repository is required");
        processed = Counter.builder("sea.outbox.processed")
                .description("Processed outbox events")
                .register(registry);
        retries = Counter.builder("sea.outbox.retries")
                .description("Outbox events scheduled for retry")
                .register(registry);
        Gauge.builder("sea.outbox.pending", repository,
                        SpringDataOutboxEventJpaRepository::countByProcessedAtIsNull)
                .description("Unprocessed outbox events")
                .register(registry);
    }

    @Override
    public void processed() {
        processed.increment();
    }

    @Override
    public void retry() {
        retries.increment();
    }
}
