package br.com.sea.solicitations.user.application.port.in;

import br.com.sea.solicitations.user.domain.UserRole;

import java.util.Objects;
import java.util.UUID;

public record RegisterClientResult(
        UUID id,
        String name,
        String email,
        UserRole role
) {

    public RegisterClientResult {
        Objects.requireNonNull(id, "Id is required");
        Objects.requireNonNull(name, "Name is required");
        Objects.requireNonNull(email, "Email is required");
        Objects.requireNonNull(role, "Role is required");
    }
}
