package br.com.sea.solicitations.user.adapter.in.web.dto;

import br.com.sea.solicitations.user.application.port.in.RegisterClientResult;
import br.com.sea.solicitations.user.domain.UserRole;

import java.util.Objects;
import java.util.UUID;

public record RegisterClientResponse(
        UUID id,
        String name,
        String email,
        UserRole role
) {

    public RegisterClientResponse {
        Objects.requireNonNull(id, "Id is required");
        Objects.requireNonNull(name, "Name is required");
        Objects.requireNonNull(email, "Email is required");
        Objects.requireNonNull(role, "Role is required");
    }

    public static RegisterClientResponse from(RegisterClientResult result) {
        Objects.requireNonNull(result, "Register client result is required");
        return new RegisterClientResponse(
                result.id(),
                result.name(),
                result.email(),
                result.role()
        );
    }
}
