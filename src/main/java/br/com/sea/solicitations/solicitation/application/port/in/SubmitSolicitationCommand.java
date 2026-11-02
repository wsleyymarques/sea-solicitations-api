package br.com.sea.solicitations.solicitation.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record SubmitSolicitationCommand(
        UUID solicitationId,
        UUID clientId
) {

    public SubmitSolicitationCommand {
        Objects.requireNonNull(solicitationId, "Solicitation ID is required");
        Objects.requireNonNull(clientId, "Client ID is required");
    }
}
