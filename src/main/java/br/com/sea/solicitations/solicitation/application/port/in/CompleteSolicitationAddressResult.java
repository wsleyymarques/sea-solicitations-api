package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.domain.Address;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;

import java.util.Objects;
import java.util.UUID;

public record CompleteSolicitationAddressResult(
        UUID id,
        SolicitationStatus status,
        SolicitationStep currentStep,
        String postalCode,
        String street,
        String neighborhood,
        String city,
        FederativeUnit state,
        String number,
        String complement
) {

    public CompleteSolicitationAddressResult {
        Objects.requireNonNull(id, "Solicitation ID is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(currentStep, "Current step is required");
        Objects.requireNonNull(postalCode, "Postal code is required");
        Objects.requireNonNull(street, "Street is required");
        Objects.requireNonNull(neighborhood, "Neighborhood is required");
        Objects.requireNonNull(city, "City is required");
        Objects.requireNonNull(state, "State is required");
        Objects.requireNonNull(number, "Address number is required");
    }

    public static CompleteSolicitationAddressResult from(
            UUID id,
            SolicitationStatus status,
            SolicitationStep currentStep,
            Address address
    ) {
        Objects.requireNonNull(address, "Address is required");
        return new CompleteSolicitationAddressResult(
                id,
                status,
                currentStep,
                address.getPostalCode(),
                address.getStreet(),
                address.getNeighborhood(),
                address.getCity(),
                address.getState(),
                address.getNumber(),
                address.getComplement()
        );
    }
}
