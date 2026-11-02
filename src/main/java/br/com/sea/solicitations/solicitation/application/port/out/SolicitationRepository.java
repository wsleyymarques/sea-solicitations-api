package br.com.sea.solicitations.solicitation.application.port.out;

import br.com.sea.solicitations.solicitation.domain.Solicitation;

import java.util.Optional;
import java.util.UUID;

public interface SolicitationRepository {

    Optional<Solicitation> findById(UUID id);

    UUID save(Solicitation solicitation);
}
