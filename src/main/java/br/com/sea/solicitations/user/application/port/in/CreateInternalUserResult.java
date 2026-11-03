package br.com.sea.solicitations.user.application.port.in;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.user.domain.UserRole;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record CreateInternalUserResult(
        UUID id,
        String name,
        String email,
        UserRole role,
        Set<FederativeUnit> coveredStates
) {

    public CreateInternalUserResult {
        Objects.requireNonNull(id, "Id is required");
        Objects.requireNonNull(name, "Name is required");
        Objects.requireNonNull(email, "Email is required");
        Objects.requireNonNull(role, "Role is required");
        coveredStates = Set.copyOf(Objects.requireNonNull(coveredStates, "Covered states are required"));
    }
}
