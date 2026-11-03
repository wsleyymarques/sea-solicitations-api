package br.com.sea.solicitations.solicitation.application.port.in;

import java.util.List;
import java.util.Objects;

public record ListAnalystSolicitationsResult(
        List<AnalystSolicitationSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public ListAnalystSolicitationsResult {
        content = List.copyOf(Objects.requireNonNull(content, "Content is required"));
    }
}
