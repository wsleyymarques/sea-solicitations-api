package br.com.sea.solicitations.outbox.adapter.out.persistence;

import br.com.sea.solicitations.outbox.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataOutboxEventJpaRepository extends JpaRepository<OutboxEvent, UUID> {

    long countByProcessedAtIsNull();

    @Query(value = """
            SELECT event.*
            FROM outbox_events event
            WHERE event.processed_at IS NULL
              AND event.next_attempt_at <= :now
              AND NOT EXISTS (
                  SELECT 1
                  FROM outbox_events older
                  WHERE older.aggregate_id = event.aggregate_id
                    AND older.processed_at IS NULL
                    AND (
                        older.occurred_at < event.occurred_at
                        OR (older.occurred_at = event.occurred_at AND older.id < event.id)
                    )
              )
            ORDER BY event.occurred_at, event.id
            LIMIT :batchSize
            FOR UPDATE OF event SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> claimReady(
            @Param("now") Instant now,
            @Param("batchSize") int batchSize
    );
}
