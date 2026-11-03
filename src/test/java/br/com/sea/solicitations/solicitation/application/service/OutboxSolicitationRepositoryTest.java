package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.outbox.application.port.out.OutboxStore;
import br.com.sea.solicitations.outbox.domain.OutboxEvent;
import br.com.sea.solicitations.outbox.domain.OutboxEventType;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxSolicitationRepositoryTest {

    @Test
    void savesSolicitationAndIndexRequestInOneTransactionalMethod() throws Exception {
        SolicitationRepository database = mock(SolicitationRepository.class);
        OutboxStore outbox = mock(OutboxStore.class);
        Instant now = Instant.parse("2026-11-02T15:00:00Z");
        OutboxSolicitationRepository repository = new OutboxSolicitationRepository(
                database, outbox, Clock.fixed(now, ZoneOffset.UTC)
        );
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        UUID id = UUID.randomUUID();
        when(database.save(solicitation)).thenReturn(id);

        assertThat(repository.save(solicitation)).isEqualTo(id);

        ArgumentCaptor<OutboxEvent> event = ArgumentCaptor.forClass(OutboxEvent.class);
        var sequence = inOrder(database, outbox);
        sequence.verify(database).save(solicitation);
        sequence.verify(outbox).save(event.capture());
        assertThat(event.getValue().getAggregateId()).isEqualTo(id);
        assertThat(event.getValue().getEventType()).isEqualTo(OutboxEventType.SOLICITATION_INDEX_REQUESTED);
        assertThat(event.getValue().getPayload()).isEmpty();
        assertThat(event.getValue().getOccurredAt()).isEqualTo(now);
        assertThat(OutboxSolicitationRepository.class.getMethod("save", Solicitation.class)
                .isAnnotationPresent(Transactional.class)).isTrue();
    }
}
