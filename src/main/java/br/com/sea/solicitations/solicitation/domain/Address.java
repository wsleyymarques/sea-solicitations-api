package br.com.sea.solicitations.solicitation.domain;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

@Embeddable
public class Address {

    @Column(name = "postal_code", length = 8)
    private String postalCode;

    @Column(length = 150)
    private String street;

    @Column(length = 100)
    private String neighborhood;

    @Column(length = 100)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 2)
    private FederativeUnit state;

    @Column(name = "address_number", length = 20)
    private String number;

    @Column(length = 120)
    private String complement;

    protected Address() {
    }

    public Address(
            String postalCode,
            String street,
            String neighborhood,
            String city,
            FederativeUnit state,
            String number,
            String complement
    ) {
        this.postalCode = normalizePostalCode(postalCode);
        this.street = requireText(street, "Street", 150);
        this.neighborhood = requireText(neighborhood, "Neighborhood", 100);
        this.city = requireText(city, "City", 100);
        this.state = Objects.requireNonNull(state, "State is required");
        this.number = requireText(number, "Address number", 20);
        this.complement = optionalText(complement, "Complement", 120);
    }

    private static String normalizePostalCode(String postalCode) {
        if (postalCode == null) {
            throw new IllegalArgumentException("Postal code is required");
        }
        String normalized = postalCode.replaceAll("\\D", "");
        if (normalized.length() != 8) {
            throw new IllegalArgumentException("Postal code must have 8 digits");
        }
        return normalized;
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
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireText(value, field, maximumLength);
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getStreet() {
        return street;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public String getCity() {
        return city;
    }

    public FederativeUnit getState() {
        return state;
    }

    public String getNumber() {
        return number;
    }

    public String getComplement() {
        return complement;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Address address)) {
            return false;
        }
        return Objects.equals(postalCode, address.postalCode)
                && Objects.equals(street, address.street)
                && Objects.equals(neighborhood, address.neighborhood)
                && Objects.equals(city, address.city)
                && state == address.state
                && Objects.equals(number, address.number)
                && Objects.equals(complement, address.complement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postalCode, street, neighborhood, city, state, number, complement);
    }
}
