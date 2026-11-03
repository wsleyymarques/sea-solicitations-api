package br.com.sea.solicitations.solicitation.application.port.in;

public interface SearchSolicitationsUseCase {
    SearchSolicitationsResult search(SearchSolicitationsQuery query);
}
