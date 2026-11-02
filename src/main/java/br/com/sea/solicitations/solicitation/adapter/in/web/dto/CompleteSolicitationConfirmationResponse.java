package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationConfirmationResult;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record CompleteSolicitationConfirmationResponse(
        UUID id,
        SolicitationStatus status,
        SolicitationStep currentStep,
        ConfirmationResponse confirmation
) {

    public CompleteSolicitationConfirmationResponse {
        Objects.requireNonNull(id, "Solicitation ID is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(currentStep, "Current step is required");
        Objects.requireNonNull(confirmation, "Confirmation is required");
    }

    public static CompleteSolicitationConfirmationResponse from(
            CompleteSolicitationConfirmationResult result
    ) {
        Objects.requireNonNull(result, "Complete solicitation confirmation result is required");
        return new CompleteSolicitationConfirmationResponse(
                result.id(),
                result.status(),
                result.currentStep(),
                new ConfirmationResponse(
                        result.priority(),
                        result.preferredDate(),
                        result.estimatedValue(),
                        result.termsAccepted()
                )
        );
    }

    public record ConfirmationResponse(
            Priority priority,
            LocalDate preferredDate,
            BigDecimal estimatedValue,
            boolean termsAccepted
    ) {
    }
}
