package br.com.sea.solicitations.outbox.application.service;

import br.com.sea.solicitations.outbox.application.port.out.OutboxStore;
import br.com.sea.solicitations.outbox.application.port.out.OutboxMetrics;
import br.com.sea.solicitations.outbox.domain.OutboxEvent;
import br.com.sea.solicitations.outbox.domain.OutboxEventType;
import br.com.sea.solicitations.solicitation.application.exception.SearchUnavailableException;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationSearchIndex;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    private static final Instant NOW = Instant.parse("2026-11-02T15:00:00Z");

    @Mock private OutboxStore store;
    @Mock private OutboxMetrics metrics;
    @Mock private SolicitationRepository solicitations;
    @Mock private SolicitationSearchIndex index;

    private OutboxProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new OutboxProcessor(store, solicitations, index,
                Clock.fixed(NOW, ZoneOffset.UTC), metrics);
    }

    @Test
    void indexesCurrentAggregateAndMarksEventProcessed() throws Exception {
        UUID id = UUID.randomUUID();
        OutboxEvent event = event(id);
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        when(store.claimReady(NOW, 20)).thenReturn(List.of(event));
        when(solicitations.findById(id)).thenReturn(Optional.of(solicitation));

        processor.processBatch();

        verify(index).index(id, solicitation);
        verify(store).save(event);
        verify(metrics).processed();
        assertThat(event.isProcessed()).isTrue();
        assertThat(event.getProcessedAt()).isEqualTo(NOW);
        assertThat(OutboxProcessor.class.getMethod("processBatch")
                .isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    void keepsFailedEventPendingAndSchedulesRetry() {
        UUID id = UUID.randomUUID();
        OutboxEvent event = event(id);
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        when(store.claimReady(NOW, 20)).thenReturn(List.of(event));
        when(solicitations.findById(id)).thenReturn(Optional.of(solicitation));
        doThrow(new SearchUnavailableException(new IllegalStateException("offline")))
                .when(index).index(id, solicitation);

        processor.processBatch();

        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(event.getLastError()).isEqualTo("Search service is temporarily unavailable");
        verify(store).save(event);
        verify(metrics).retry();
    }

    @Test
    void completesOrphanedEventWithoutCallingElasticsearch() {
        UUID id = UUID.randomUUID();
        OutboxEvent event = event(id);
        when(store.claimReady(NOW, 20)).thenReturn(List.of(event));
        when(solicitations.findById(id)).thenReturn(Optional.empty());

        processor.processBatch();

        assertThat(event.isProcessed()).isTrue();
        verifyNoInteractions(index);
        verify(store).save(event);
        verify(metrics).processed();
    }

    private static OutboxEvent event(UUID id) {
        return new OutboxEvent(id, OutboxEventType.SOLICITATION_INDEX_REQUESTED,
                Map.of(), NOW.minusSeconds(60));
    }
}
