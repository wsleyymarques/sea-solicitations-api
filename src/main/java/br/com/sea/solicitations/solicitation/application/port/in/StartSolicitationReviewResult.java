package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StartSolicitationReviewResult(
        UUID id,
        SolicitationStatus status,
        UUID assignedAnalystId,
        Instant reviewStartedAt
) {

    public StartSolicitationReviewResult {
        Objects.requireNonNull(id, "Solicitation ID is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(assignedAnalystId, "Assigned analyst ID is required");
        Objects.requireNonNull(reviewStartedAt, "Review start time is required");
    }
}
