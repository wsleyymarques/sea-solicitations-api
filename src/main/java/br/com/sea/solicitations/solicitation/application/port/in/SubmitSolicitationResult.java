package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SubmitSolicitationResult(
        UUID id,
        SolicitationStatus status,
        SolicitationStep currentStep,
        Instant submittedAt
) {

    public SubmitSolicitationResult {
        Objects.requireNonNull(id, "Solicitation ID is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(currentStep, "Current step is required");
        Objects.requireNonNull(submittedAt, "Submission time is required");
    }
}
