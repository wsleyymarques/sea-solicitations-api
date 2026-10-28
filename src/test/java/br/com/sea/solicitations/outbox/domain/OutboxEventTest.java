package br.com.sea.solicitations.outbox.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEventTest {

    @Test
    void tracksFailuresUntilTheEventIsProcessed() {
        Map<String, Object> payload = new HashMap<>(Map.of("status", "SUBMITTED"));
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                OutboxEventType.SOLICITATION_INDEX_REQUESTED,
                payload,
                Instant.parse("2026-10-27T12:00:00Z")
        );
        payload.clear();

        event.registerFailure("Elasticsearch unavailable");

        assertThat(event.getPayload()).containsEntry("status", "SUBMITTED");
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.isProcessed()).isFalse();

        event.markProcessed(Instant.parse("2026-10-27T12:01:00Z"));

        assertThat(event.isProcessed()).isTrue();
        assertThat(event.getLastError()).isNull();
        assertThatThrownBy(() -> event.registerFailure("late failure"))
                .isInstanceOf(IllegalStateException.class);
    }
}
