package br.com.sea.solicitations.outbox.adapter.out.persistence;

import br.com.sea.solicitations.outbox.application.port.out.OutboxStore;
import br.com.sea.solicitations.outbox.domain.OutboxEvent;
import br.com.sea.solicitations.outbox.domain.OutboxEventType;
import br.com.sea.solicitations.solicitation.adapter.out.persistence.JpaSolicitationRepositoryAdapter;
import br.com.sea.solicitations.solicitation.application.service.OutboxSolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaSolicitationRepositoryAdapter.class, JpaOutboxStore.class,
        OutboxPersistenceIntegrationTest.TestConfig.class})
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION", matches = "1")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxPersistenceIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-02T12:00:00Z");

    @Autowired
    private OutboxSolicitationRepository repository;

    @Autowired
    private JpaOutboxStore outbox;

    @Autowired
    private FailingOutboxStore failingOutbox;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void commitsSolicitationAndEventTogether() {
        UUID clientId = insertClient();

        UUID id = repository.save(new Solicitation(clientId));

        assertThat(count("solicitations", id)).isEqualTo(1);
        assertThat(count("outbox_events", id)).isEqualTo(1);
        assertThat(claimFor(NOW, id))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getAggregateId()).isEqualTo(id);
                    assertThat(event.getEventType()).isEqualTo(OutboxEventType.SOLICITATION_INDEX_REQUESTED);
                });
    }

    @Test
    void rollsBackBothRecordsWhenTransactionFailsAfterEventWrite() {
        UUID clientId = insertClient();
        int solicitationsBefore = jdbc.queryForObject("SELECT count(*) FROM solicitations", Integer.class);
        int eventsBefore = jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class);
        failingOutbox.failNextSave();

        assertThatThrownBy(() -> repository.save(new Solicitation(clientId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Simulated outbox failure after event write");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM solicitations", Integer.class))
                .isEqualTo(solicitationsBefore);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class))
                .isEqualTo(eventsBefore);
    }

    @Test
    void claimPreservesAggregateOrderAndRetrySchedule() {
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent first = new OutboxEvent(aggregateId,
                OutboxEventType.SOLICITATION_INDEX_REQUESTED, Map.of(), NOW);
        OutboxEvent second = new OutboxEvent(aggregateId,
                OutboxEventType.SOLICITATION_INDEX_REQUESTED, Map.of(), NOW.plusSeconds(1));
        transactions.executeWithoutResult(status -> {
            outbox.save(first);
            outbox.save(second);
        });

        assertThat(claimFor(NOW.plusSeconds(2), aggregateId))
                .extracting(OutboxEvent::getId).containsExactly(first.getId());

        transactions.executeWithoutResult(status -> {
            OutboxEvent claimed = readyFor(NOW.plusSeconds(2), aggregateId);
            claimed.registerFailure("Elasticsearch unavailable", NOW.plusSeconds(2));
            outbox.save(claimed);
        });
        assertThat(claimFor(NOW.plusSeconds(6), aggregateId)).isEmpty();
        assertThat(claimFor(NOW.plusSeconds(7), aggregateId))
                .extracting(OutboxEvent::getId).containsExactly(first.getId());

        transactions.executeWithoutResult(status -> {
            OutboxEvent claimed = readyFor(NOW.plusSeconds(7), aggregateId);
            claimed.markProcessed(NOW.plusSeconds(7));
            outbox.save(claimed);
        });
        assertThat(claimFor(NOW.plusSeconds(7), aggregateId))
                .extracting(OutboxEvent::getId).containsExactly(second.getId());
    }

    @Test
    void skipsAnEventLockedByAnotherWorker() {
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent(aggregateId,
                OutboxEventType.SOLICITATION_INDEX_REQUESTED, Map.of(), NOW);
        transactions.executeWithoutResult(status -> outbox.save(event));

        transactions.executeWithoutResult(status -> {
            assertThat(readyFor(NOW, aggregateId).getId()).isEqualTo(event.getId());
            CompletableFuture<List<OutboxEvent>> otherWorker = CompletableFuture.supplyAsync(
                    () -> claimFor(NOW, aggregateId));
            assertThat(otherWorker.orTimeout(5, TimeUnit.SECONDS).join()).isEmpty();
        });

        assertThat(claimFor(NOW, aggregateId))
                .extracting(OutboxEvent::getId).containsExactly(event.getId());
    }

    private UUID insertClient() {
        UUID clientId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, full_name, email, password_hash, role, active,
                                   created_at, updated_at, version)
                VALUES (?, 'Outbox Client', ?, 'not-used', 'CLIENT', TRUE, ?, ?, 0)
                """, clientId, "outbox-" + clientId + "@example.com",
                Timestamp.from(NOW), Timestamp.from(NOW));
        return clientId;
    }

    private List<OutboxEvent> claim(Instant time) {
        return transactions.execute(status -> outbox.claimReady(time, 20));
    }

    private List<OutboxEvent> claimFor(Instant time, UUID aggregateId) {
        return claim(time).stream()
                .filter(event -> event.getAggregateId().equals(aggregateId))
                .toList();
    }

    private OutboxEvent readyFor(Instant time, UUID aggregateId) {
        return outbox.claimReady(time, 20).stream()
                .filter(event -> event.getAggregateId().equals(aggregateId))
                .findFirst()
                .orElseThrow();
    }

    private int count(String table, UUID aggregateId) {
        String column = table.equals("solicitations") ? "id" : "aggregate_id";
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE " + column + " = ?",
                Integer.class, aggregateId);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        FailingOutboxStore failingOutboxStore(JpaOutboxStore database) {
            return new FailingOutboxStore(database);
        }

        @Bean
        OutboxSolicitationRepository outboxSolicitationRepository(
                JpaSolicitationRepositoryAdapter database, FailingOutboxStore outbox, Clock clock
        ) {
            return new OutboxSolicitationRepository(database, outbox, clock);
        }
    }

    static class FailingOutboxStore implements OutboxStore {
        private final JpaOutboxStore database;
        private final AtomicBoolean failNextSave = new AtomicBoolean();

        FailingOutboxStore(JpaOutboxStore database) {
            this.database = database;
        }

        void failNextSave() {
            failNextSave.set(true);
        }

        @Override
        public void save(OutboxEvent event) {
            database.save(event);
            if (failNextSave.getAndSet(false)) {
                throw new IllegalStateException("Simulated outbox failure after event write");
            }
        }

        @Override
        public List<OutboxEvent> claimReady(Instant now, int limit) {
            return database.claimReady(now, limit);
        }
    }
}
