package br.com.sea.solicitations.outbox.adapter.out.metrics;

import br.com.sea.solicitations.outbox.adapter.out.persistence.SpringDataOutboxEventJpaRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MicrometerOutboxMetricsTest {

    @Test
    void reportsPendingEventsAndProcessingResultsWithoutDynamicTags() {
        SpringDataOutboxEventJpaRepository repository = mock(SpringDataOutboxEventJpaRepository.class);
        when(repository.countByProcessedAtIsNull()).thenReturn(3L);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerOutboxMetrics metrics = new MicrometerOutboxMetrics(registry, repository);

        metrics.processed();
        metrics.retry();
        metrics.retry();

        assertThat(registry.get("sea.outbox.pending").gauge().value()).isEqualTo(3);
        assertThat(registry.get("sea.outbox.processed").counter().count()).isEqualTo(1);
        assertThat(registry.get("sea.outbox.retries").counter().count()).isEqualTo(2);
        assertThat(registry.get("sea.outbox.retries").counter().getId().getTags()).isEmpty();
    }
}
