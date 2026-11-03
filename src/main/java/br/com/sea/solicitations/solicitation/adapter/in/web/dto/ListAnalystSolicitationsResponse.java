package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.solicitation.application.port.in.ListAnalystSolicitationsResult;

import java.util.List;
import java.util.Objects;

public record ListAnalystSolicitationsResponse(
        List<AnalystSolicitationSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ListAnalystSolicitationsResponse from(ListAnalystSolicitationsResult result) {
        Objects.requireNonNull(result, "List analyst solicitations result is required");
        return new ListAnalystSolicitationsResponse(
                result.content().stream().map(AnalystSolicitationSummaryResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
