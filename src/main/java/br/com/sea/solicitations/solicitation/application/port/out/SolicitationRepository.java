package br.com.sea.solicitations.solicitation.application.port.out;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.domain.Solicitation;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SolicitationRepository {

    Optional<Solicitation> findById(UUID id);

    SolicitationPage findVisibleToAnalyst(
            UUID analystId,
            Set<FederativeUnit> coveredStates,
            int page,
            int size
    );

    UUID save(Solicitation solicitation);
}
