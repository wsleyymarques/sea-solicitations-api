package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.solicitation.domain.Confirmation;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record CompleteSolicitationConfirmationResult(
        UUID id,
        SolicitationStatus status,
        SolicitationStep currentStep,
        Priority priority,
        LocalDate preferredDate,
        BigDecimal estimatedValue,
        boolean termsAccepted
) {

    public CompleteSolicitationConfirmationResult {
        Objects.requireNonNull(id, "Solicitation ID is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(currentStep, "Current step is required");
        Objects.requireNonNull(priority, "Priority is required");
        Objects.requireNonNull(preferredDate, "Preferred date is required");
    }

    public static CompleteSolicitationConfirmationResult from(
            UUID id,
            SolicitationStatus status,
            SolicitationStep currentStep,
            Confirmation confirmation
    ) {
        Objects.requireNonNull(confirmation, "Confirmation is required");
        return new CompleteSolicitationConfirmationResult(
                id,
                status,
                currentStep,
                confirmation.getPriority(),
                confirmation.getPreferredDate(),
                confirmation.getEstimatedValue(),
                confirmation.isTermsAccepted()
        );
    }
}
