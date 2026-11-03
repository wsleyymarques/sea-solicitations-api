package br.com.sea.solicitations.solicitation.application.port.out;

import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsQuery;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsResult;
import br.com.sea.solicitations.solicitation.domain.Solicitation;

import java.util.UUID;

public interface SolicitationSearchIndex {
    void index(UUID id, Solicitation solicitation);

    SearchSolicitationsResult search(SearchSolicitationsQuery query);
}
