package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompleteSolicitationAddressRequest(
        @NotBlank(message = "Postal code is required")
        @Pattern(
                regexp = "\\d{5}-?\\d{3}",
                message = "Postal code must have 8 digits, optionally with a hyphen"
        )
        String postalCode,

        @NotBlank(message = "Address number is required")
        @Size(max = 20, message = "Address number must have at most 20 characters")
        String number,

        @Size(max = 120, message = "Complement must have at most 120 characters")
        String complement
) {

    public CompleteSolicitationAddressRequest {
        postalCode = normalize(postalCode);
        number = normalize(number);
        complement = normalizeOptional(complement);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        String normalized = normalize(value);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
