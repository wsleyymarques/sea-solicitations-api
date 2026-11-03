package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.audit.application.annotation.Audited;
import br.com.sea.solicitations.audit.domain.AuditAction;
import br.com.sea.solicitations.solicitation.application.exception.InvalidSolicitationStateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressResult;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressUseCase;
import br.com.sea.solicitations.solicitation.application.port.out.PostalCodeData;
import br.com.sea.solicitations.solicitation.application.port.out.PostalCodeLookup;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Address;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.util.Objects;
import java.util.UUID;

public class CompleteSolicitationAddressService implements CompleteSolicitationAddressUseCase {

    private final SolicitationRepository solicitationRepository;
    private final PostalCodeLookup postalCodeLookup;

    public CompleteSolicitationAddressService(
            SolicitationRepository solicitationRepository,
            PostalCodeLookup postalCodeLookup
    ) {
        this.solicitationRepository = Objects.requireNonNull(
                solicitationRepository,
                "Solicitation repository is required"
        );
        this.postalCodeLookup = Objects.requireNonNull(
                postalCodeLookup,
                "Postal code lookup is required"
        );
    }

    @Override
    @Audited(AuditAction.ADDRESS_SAVED)
    public CompleteSolicitationAddressResult complete(CompleteSolicitationAddressCommand command) {
        Objects.requireNonNull(command, "Complete solicitation address command is required");

        Solicitation solicitation = solicitationRepository
                .findById(command.solicitationId())
                .orElseThrow(SolicitationNotFoundException::new);

        if (!solicitation.isOwnedBy(command.clientId())) {
            throw new SolicitationAccessDeniedException();
        }
        if (solicitation.getStatus() != SolicitationStatus.DRAFT) {
            throw new InvalidSolicitationStateException("Only draft solicitations can be edited");
        }
        if (solicitation.getBasicData() == null) {
            throw new InvalidSolicitationStateException(
                    "Basic data must be completed before the address"
            );
        }

        PostalCodeData postalCode = postalCodeLookup.find(command.postalCode());
        Address address = new Address(
                postalCode.postalCode(),
                postalCode.street(),
                postalCode.neighborhood(),
                postalCode.city(),
                postalCode.state(),
                command.number(),
                command.complement()
        );
        solicitation.completeAddress(address);

        UUID savedId = solicitationRepository.save(solicitation);
        return CompleteSolicitationAddressResult.from(
                savedId,
                solicitation.getStatus(),
                solicitation.getCurrentStep(),
                address
        );
    }
}
