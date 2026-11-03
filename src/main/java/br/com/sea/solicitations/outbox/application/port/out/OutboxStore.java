package br.com.sea.solicitations.outbox.application.port.out;

import br.com.sea.solicitations.outbox.domain.OutboxEvent;

import java.time.Instant;
import java.util.List;

public interface OutboxStore {
    void save(OutboxEvent event);

    List<OutboxEvent> claimReady(Instant now, int limit);
}
