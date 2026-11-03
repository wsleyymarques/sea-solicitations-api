package br.com.sea.solicitations.outbox.adapter.out.persistence;

import br.com.sea.solicitations.outbox.domain.OutboxEvent;
import br.com.sea.solicitations.outbox.domain.OutboxEventType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaOutboxStoreTest {

    @Test
    void delegatesSaveAndReadyClaimWithBoundedBatch() throws Exception {
        SpringDataOutboxEventJpaRepository jpa = mock(SpringDataOutboxEventJpaRepository.class);
        JpaOutboxStore store = new JpaOutboxStore(jpa);
        Instant now = Instant.parse("2026-11-02T15:00:00Z");
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), OutboxEventType.SOLICITATION_INDEX_REQUESTED, Map.of(), now
        );
        when(jpa.claimReady(now, 20)).thenReturn(List.of(event));

        store.save(event);
        assertThat(store.claimReady(now, 20)).containsExactly(event);

        verify(jpa).save(event);
        verify(jpa).claimReady(now, 20);
        assertThat(SpringDataOutboxEventJpaRepository.class
                .getMethod("claimReady", Instant.class, int.class)
                .getAnnotation(Query.class).value())
                .contains("FOR UPDATE OF event SKIP LOCKED", "NOT EXISTS", "next_attempt_at");
    }
}
