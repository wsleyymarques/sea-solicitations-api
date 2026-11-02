package br.com.sea.solicitations.solicitation.application.port.out;

import br.com.sea.solicitations.solicitation.domain.Solicitation;

import java.util.UUID;

public interface SolicitationRepository {

    UUID save(Solicitation solicitation);
}
