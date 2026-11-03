package br.com.sea.solicitations.solicitation.application.port.in;

import java.util.List;
import java.util.Objects;

public record SearchSolicitationsResult(
        List<SearchSolicitationSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public SearchSolicitationsResult {
        content = List.copyOf(Objects.requireNonNull(content, "Content is required"));
    }
}
