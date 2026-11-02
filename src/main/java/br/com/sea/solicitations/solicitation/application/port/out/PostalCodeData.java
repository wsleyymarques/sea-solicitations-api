package br.com.sea.solicitations.solicitation.application.port.out;

import br.com.sea.solicitations.shared.domain.FederativeUnit;

import java.util.Objects;

public record PostalCodeData(
        String postalCode,
        String street,
        String neighborhood,
        String city,
        FederativeUnit state
) {

    public PostalCodeData {
        postalCode = requireText(postalCode, "Postal code");
        street = requireText(street, "Street");
        neighborhood = requireText(neighborhood, "Neighborhood");
        city = requireText(city, "City");
        Objects.requireNonNull(state, "State is required");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
