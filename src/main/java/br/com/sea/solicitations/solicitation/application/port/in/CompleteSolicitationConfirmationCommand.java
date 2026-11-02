package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.solicitation.domain.Priority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record CompleteSolicitationConfirmationCommand(
        UUID solicitationId,
        UUID clientId,
        Priority priority,
        LocalDate preferredDate,
        BigDecimal estimatedValue,
        boolean termsAccepted
) {

    public CompleteSolicitationConfirmationCommand {
        Objects.requireNonNull(solicitationId, "Solicitation ID is required");
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(priority, "Priority is required");
        Objects.requireNonNull(preferredDate, "Preferred date is required");
    }
}
