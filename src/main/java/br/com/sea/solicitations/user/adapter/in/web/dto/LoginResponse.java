package br.com.sea.solicitations.user.adapter.in.web.dto;

import br.com.sea.solicitations.user.application.port.in.AuthenticationResult;

import java.util.Objects;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {

    public LoginResponse {
        Objects.requireNonNull(accessToken, "Access token is required");
        Objects.requireNonNull(tokenType, "Token type is required");
    }

    public static LoginResponse from(AuthenticationResult result) {
        Objects.requireNonNull(result, "Authentication result is required");
        return new LoginResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresIn()
        );
    }

    @Override
    public String toString() {
        return "LoginResponse[accessToken=[REDACTED], tokenType=" + tokenType +
                ", expiresIn=" + expiresIn + ']';
    }
}
