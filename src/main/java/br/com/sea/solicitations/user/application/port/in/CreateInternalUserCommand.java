package br.com.sea.solicitations.user.application.port.in;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.user.domain.UserRole;

import java.util.Objects;
import java.util.Set;

public record CreateInternalUserCommand(
        String name,
        String email,
        String rawPassword,
        UserRole role,
        Set<FederativeUnit> coveredStates
) {

    public CreateInternalUserCommand {
        Objects.requireNonNull(name, "Name is required");
        Objects.requireNonNull(email, "Email is required");
        Objects.requireNonNull(rawPassword, "Raw password is required");
        Objects.requireNonNull(role, "Role is required");
        coveredStates = Set.copyOf(Objects.requireNonNull(coveredStates, "Covered states are required"));

        if (role == UserRole.CLIENT) {
            throw new IllegalArgumentException("Only analysts and administrators can be created");
        }
        if (role == UserRole.ANALYST && coveredStates.isEmpty()) {
            throw new IllegalArgumentException("Analyst coverage must contain at least one state");
        }
        if (role == UserRole.ADMIN && !coveredStates.isEmpty()) {
            throw new IllegalArgumentException("Administrators cannot have state coverage");
        }
    }

    @Override
    public String toString() {
        return "CreateInternalUserCommand["
                + "name=" + name
                + ", email=" + email
                + ", rawPassword=[REDACTED]"
                + ", role=" + role
                + ", coveredStates=" + coveredStates
                + "]";
    }
}
