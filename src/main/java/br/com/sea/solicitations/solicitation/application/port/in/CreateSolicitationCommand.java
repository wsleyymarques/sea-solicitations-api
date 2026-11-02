package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.solicitation.domain.ServiceType;

import java.util.Objects;
import java.util.UUID;

public record CreateSolicitationCommand(
        UUID clientId,
        ServiceType serviceType,
        String title,
        String description
) {
    public CreateSolicitationCommand {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(serviceType, "Service type is required");
        Objects.requireNonNull(title, "Title is required");
        Objects.requireNonNull(description, "Description is required");
    }
}
