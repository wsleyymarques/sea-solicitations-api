package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AnalystSolicitationSummary(
        UUID id,
        SolicitationStatus status,
        ServiceType serviceType,
        String title,
        Priority priority,
        FederativeUnit state,
        Instant submittedAt,
        UUID assignedAnalystId,
        Instant reviewStartedAt
) {

    public AnalystSolicitationSummary {
        Objects.requireNonNull(id, "Solicitation ID is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(serviceType, "Service type is required");
        Objects.requireNonNull(title, "Title is required");
        Objects.requireNonNull(priority, "Priority is required");
        Objects.requireNonNull(state, "State is required");
        Objects.requireNonNull(submittedAt, "Submission time is required");
    }
}
