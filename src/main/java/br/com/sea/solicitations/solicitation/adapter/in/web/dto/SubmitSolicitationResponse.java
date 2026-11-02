package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.solicitation.application.port.in.SubmitSolicitationResult;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SubmitSolicitationResponse(
        UUID id,
        SolicitationStatus status,
        SolicitationStep currentStep,
        Instant submittedAt
) {

    public SubmitSolicitationResponse {
        Objects.requireNonNull(id, "Solicitation ID is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(currentStep, "Current step is required");
        Objects.requireNonNull(submittedAt, "Submission time is required");
    }

    public static SubmitSolicitationResponse from(SubmitSolicitationResult result) {
        Objects.requireNonNull(result, "Submit solicitation result is required");
        return new SubmitSolicitationResponse(
                result.id(),
                result.status(),
                result.currentStep(),
                result.submittedAt()
        );
    }
}
