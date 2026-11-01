package br.com.sea.solicitations.user.application.port.out;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.user.domain.UserRole;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Authentication data that the application requires in an access token.
 */
public record AccessTokenClaims(
        UUID userId,
        UserRole role,
        Set<FederativeUnit> coveredStates
) {

    public AccessTokenClaims {
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(role, "User role is required");
        coveredStates = Set.copyOf(
                Objects.requireNonNull(coveredStates, "Covered states are required")
        );
    }
}
