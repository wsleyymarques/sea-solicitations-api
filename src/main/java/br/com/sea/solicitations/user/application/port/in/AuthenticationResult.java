package br.com.sea.solicitations.user.application.port.in;

import java.util.Objects;


public record AuthenticationResult(
    String accessToken,
    String tokenType,
    long expiresIn
) {

    public AuthenticationResult {
        Objects.requireNonNull(accessToken, "Access token is required");
        Objects.requireNonNull(tokenType, "Token type is required");

        if (accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token cannot be blank");
        }

        if (tokenType.isBlank()) {
            throw new IllegalArgumentException("Token type cannot be blank");
        }

        if (expiresIn <= 0) {
            throw new IllegalArgumentException(
                "Expiration time must be greater than zero"
            );
        }
    }

    @Override
    public String toString() {
        return "AuthenticationResult[" +
            "accessToken=REDACTED, " +
            "tokenType=" + tokenType + ", " +
            "expiresIn=" + expiresIn +
            ']';
    }
}
