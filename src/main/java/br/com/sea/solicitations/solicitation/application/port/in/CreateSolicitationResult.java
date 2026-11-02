package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;

import java.util.Objects;
import java.util.UUID;

public record CreateSolicitationResult(
        UUID id,
        SolicitationStatus status,
        SolicitationStep currentStep
) {

    public CreateSolicitationResult {
        Objects.requireNonNull(id, "Solicitation ID is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(currentStep, "Current step is required");
    }
}
