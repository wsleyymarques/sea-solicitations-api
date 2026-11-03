package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record SearchSolicitationsQuery(
        UUID analystId,
        Set<FederativeUnit> coveredStates,
        String text,
        SolicitationStatus status,
        ServiceType serviceType,
        Priority priority,
        FederativeUnit state,
        LocalDate submittedFrom,
        LocalDate submittedTo,
        int page,
        int size,
        SearchSortField sortBy,
        SearchDirection direction
) {
    public SearchSolicitationsQuery {
        Objects.requireNonNull(analystId, "Analyst ID is required");
        coveredStates = Set.copyOf(Objects.requireNonNull(coveredStates, "Covered states are required"));
        Objects.requireNonNull(sortBy, "Sort field is required");
        Objects.requireNonNull(direction, "Sort direction is required");
        if (page < 0 || size < 1 || size > 100 || ((long) page + 1) * size > 10_000) {
            throw new IllegalArgumentException("Page and size must fit within the first 10000 results");
        }
        if (submittedFrom != null && submittedTo != null && submittedFrom.isAfter(submittedTo)) {
            throw new IllegalArgumentException("Submitted from must not be after submitted to");
        }
        if (text != null) {
            text = text.trim();
            if (text.length() > 200) {
                throw new IllegalArgumentException("Search text must have at most 200 characters");
            }
            if (text.isEmpty()) {
                text = null;
            }
        }
    }
}
