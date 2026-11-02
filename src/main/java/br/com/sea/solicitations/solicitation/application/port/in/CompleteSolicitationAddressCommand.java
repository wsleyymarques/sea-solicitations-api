package br.com.sea.solicitations.solicitation.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record CompleteSolicitationAddressCommand(
        UUID solicitationId,
        UUID clientId,
        String postalCode,
        String number,
        String complement
) {

    public CompleteSolicitationAddressCommand {
        Objects.requireNonNull(solicitationId, "Solicitation ID is required");
        Objects.requireNonNull(clientId, "Client ID is required");
        postalCode = normalizePostalCode(postalCode);
        number = requireText(number, "Address number", 20);
        complement = optionalText(complement, "Complement", 120);
    }

    private static String normalizePostalCode(String value) {
        if (value == null || !value.trim().matches("\\d{5}-?\\d{3}")) {
            throw new IllegalArgumentException("Postal code must have 8 digits");
        }
        return value.trim().replace("-", "");
    }

    private static String requireText(String value, String field, int maximumLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(field + " must have at most " + maximumLength + " characters");
        }
        return normalized;
    }

    private static String optionalText(String value, String field, int maximumLength) {
        return value == null || value.isBlank()
                ? null
                : requireText(value, field, maximumLength);
    }
}
