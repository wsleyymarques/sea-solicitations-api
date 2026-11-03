package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsResult;

import java.util.List;

public record SearchSolicitationsResponse(
        List<SearchSolicitationSummaryResponse> items,
        int page,
        int size,
        long total
) {
    public static SearchSolicitationsResponse from(SearchSolicitationsResult result) {
        return new SearchSolicitationsResponse(
                result.content().stream().map(SearchSolicitationSummaryResponse::from).toList(),
                result.page(), result.size(), result.totalElements()
        );
    }
}
