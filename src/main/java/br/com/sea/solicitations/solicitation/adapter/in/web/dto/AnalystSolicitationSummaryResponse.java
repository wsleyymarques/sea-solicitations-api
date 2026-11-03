package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.port.in.AnalystSolicitationSummary;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AnalystSolicitationSummaryResponse(
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

    public static AnalystSolicitationSummaryResponse from(AnalystSolicitationSummary summary) {
        Objects.requireNonNull(summary, "Analyst solicitation summary is required");
        return new AnalystSolicitationSummaryResponse(
                summary.id(),
                summary.status(),
                summary.serviceType(),
                summary.title(),
                summary.priority(),
                summary.state(),
                summary.submittedAt(),
                summary.assignedAnalystId(),
                summary.reviewStartedAt()
        );
    }
}
