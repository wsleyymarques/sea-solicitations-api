package br.com.sea.solicitations.user.application.port.in;

import java.util.Locale;
import java.util.Objects;

public record AuthenticationCommand(
    String email,
    String rawPassword
) {

    public AuthenticationCommand {
        email = Objects.requireNonNull(
            email,
            "Email is required"
        ).trim().toLowerCase(Locale.ROOT);

        rawPassword = Objects.requireNonNull(
            rawPassword,
            "Raw password is required"
        );

        if (email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        if (rawPassword.isBlank()) {
            throw new IllegalArgumentException("Raw password cannot be blank");
        }
    }

    @Override
    public String toString() {
        return "AuthenticationCommand[" +
            "email=" + email +
            ", rawPassword=REDACTED" +
            ']';
    }
}
