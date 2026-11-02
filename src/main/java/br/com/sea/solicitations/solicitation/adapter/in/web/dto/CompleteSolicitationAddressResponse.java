package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressResult;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;

import java.util.Objects;
import java.util.UUID;

public record CompleteSolicitationAddressResponse(
        UUID id,
        SolicitationStatus status,
        SolicitationStep currentStep,
        AddressResponse address
) {

    public CompleteSolicitationAddressResponse {
        Objects.requireNonNull(id, "Solicitation ID is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(currentStep, "Current step is required");
        Objects.requireNonNull(address, "Address is required");
    }

    public static CompleteSolicitationAddressResponse from(
            CompleteSolicitationAddressResult result
    ) {
        Objects.requireNonNull(result, "Complete solicitation address result is required");
        return new CompleteSolicitationAddressResponse(
                result.id(),
                result.status(),
                result.currentStep(),
                new AddressResponse(
                        result.postalCode(),
                        result.street(),
                        result.neighborhood(),
                        result.city(),
                        result.state(),
                        result.number(),
                        result.complement()
                )
        );
    }

    public record AddressResponse(
            String postalCode,
            String street,
            String neighborhood,
            String city,
            FederativeUnit state,
            String number,
            String complement
    ) {
    }
}
