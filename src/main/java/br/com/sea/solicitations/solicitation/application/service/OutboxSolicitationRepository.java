package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.outbox.application.port.out.OutboxStore;
import br.com.sea.solicitations.outbox.domain.OutboxEvent;
import br.com.sea.solicitations.outbox.domain.OutboxEventType;
import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationPage;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class OutboxSolicitationRepository implements SolicitationRepository {

    private final SolicitationRepository database;
    private final OutboxStore outbox;
    private final Clock clock;

    public OutboxSolicitationRepository(
            SolicitationRepository database,
            OutboxStore outbox,
            Clock clock
    ) {
        this.database = Objects.requireNonNull(database, "Database repository is required");
        this.outbox = Objects.requireNonNull(outbox, "Outbox store is required");
        this.clock = Objects.requireNonNull(clock, "Clock is required");
    }

    @Override
    public Optional<Solicitation> findById(UUID id) {
        return database.findById(id);
    }

    @Override
    public SolicitationPage findVisibleToAnalyst(
            UUID analystId, Set<FederativeUnit> coveredStates, int page, int size
    ) {
        return database.findVisibleToAnalyst(analystId, coveredStates, page, size);
    }

    @Override
    @Transactional
    public UUID save(Solicitation solicitation) {
        UUID id = database.save(solicitation);
        outbox.save(new OutboxEvent(
                id,
                OutboxEventType.SOLICITATION_INDEX_REQUESTED,
                Map.of(),
                clock.instant()
        ));
        return id;
    }
}
