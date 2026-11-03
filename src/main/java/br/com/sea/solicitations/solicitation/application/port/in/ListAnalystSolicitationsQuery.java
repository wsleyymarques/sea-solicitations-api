package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.shared.domain.FederativeUnit;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ListAnalystSolicitationsQuery(
        UUID analystId,
        Set<FederativeUnit> coveredStates,
        int page,
        int size
) {

    public ListAnalystSolicitationsQuery {
        Objects.requireNonNull(analystId, "Analyst ID is required");
        coveredStates = Set.copyOf(Objects.requireNonNull(coveredStates, "Covered states are required"));
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }
}
