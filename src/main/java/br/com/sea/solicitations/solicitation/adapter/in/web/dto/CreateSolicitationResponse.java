package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationResult;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;

import java.util.Objects;
import java.util.UUID;

public record CreateSolicitationResponse(
        UUID id,
        SolicitationStatus status,
        SolicitationStep currentStep
) {

    public CreateSolicitationResponse {
        Objects.requireNonNull(id, "Solicitation ID is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(currentStep, "Current step is required");
    }

    public static CreateSolicitationResponse from(CreateSolicitationResult result) {
        Objects.requireNonNull(result, "Create solicitation result is required");
        return new CreateSolicitationResponse(
                result.id(),
                result.status(),
                result.currentStep()
        );
    }
}
