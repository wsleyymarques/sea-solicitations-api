package br.com.sea.solicitations.user.application.port.in;

import java.util.Objects;

public record RegisterClientCommand(
        String name,
        String email,
        String rawPassword
) {

    public RegisterClientCommand {
        Objects.requireNonNull(name, "Name is required");
        Objects.requireNonNull(email, "Email is required");
        Objects.requireNonNull(rawPassword, "Raw password is required");
    }

    @Override
    public String toString() {
        return "RegisterClientCommand["
                + "name=" + name
                + ", email=" + email
                + ", rawPassword=[REDACTED]]";
    }
}
