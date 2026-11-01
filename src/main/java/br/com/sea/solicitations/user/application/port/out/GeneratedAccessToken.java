package br.com.sea.solicitations.user.application.port.out;

import java.util.Objects;

/**
 * Access token created by the token generator and its lifetime in seconds.
 */
public record GeneratedAccessToken(
        String value,
        long expiresIn
) {

    public GeneratedAccessToken {
        Objects.requireNonNull(value, "Access token is required");

        if (value.isBlank()) {
            throw new IllegalArgumentException("Access token cannot be blank");
        }

        if (expiresIn <= 0) {
            throw new IllegalArgumentException("Expiration time must be greater than zero");
        }
    }

    @Override
    public String toString() {
        return "GeneratedAccessToken[value=REDACTED, expiresIn=" + expiresIn + ']';
    }
}
