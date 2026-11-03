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

        event.registerFailure("Elasticsearch unavailable", Instant.parse("2026-10-27T12:00:05Z"));

        assertThat(event.getPayload()).containsEntry("status", "SUBMITTED");
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.isProcessed()).isFalse();

        event.markProcessed(Instant.parse("2026-10-27T12:01:00Z"));

        assertThat(event.isProcessed()).isTrue();
        assertThat(event.getLastError()).isNull();
        assertThatThrownBy(() -> event.registerFailure("late failure", Instant.parse("2026-10-27T12:02:00Z")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void schedulesExponentialRetryWithFiveMinuteCap() {
        Instant now = Instant.parse("2026-11-02T15:00:00Z");
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), OutboxEventType.SOLICITATION_INDEX_REQUESTED, Map.of(), now
        );
        assertThat(event.getNextAttemptAt()).isEqualTo(now);

        event.registerFailure("temporary failure", now);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(now.plusSeconds(5));

        event.registerFailure("temporary failure", now);
        assertThat(event.getAttempts()).isEqualTo(2);
        assertThat(event.getNextAttemptAt()).isEqualTo(now.plusSeconds(10));

        for (int attempt = 3; attempt <= 20; attempt++) {
            event.registerFailure("temporary failure", now);
        }
        assertThat(event.getNextAttemptAt()).isEqualTo(now.plusSeconds(300));
    }
}
