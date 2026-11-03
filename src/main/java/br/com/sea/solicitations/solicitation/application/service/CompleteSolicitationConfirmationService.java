package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.audit.application.annotation.Audited;
import br.com.sea.solicitations.audit.domain.AuditAction;
import br.com.sea.solicitations.solicitation.application.exception.InvalidSolicitationStateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationConfirmationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationConfirmationResult;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationConfirmationUseCase;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Confirmation;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class CompleteSolicitationConfirmationService implements CompleteSolicitationConfirmationUseCase {

    private final SolicitationRepository solicitationRepository;
    private final Clock clock;

    public CompleteSolicitationConfirmationService(
            SolicitationRepository solicitationRepository,
            Clock clock
    ) {
        this.solicitationRepository = Objects.requireNonNull(
                solicitationRepository,
                "Solicitation repository is required"
        );
        this.clock = Objects.requireNonNull(clock, "Clock is required");
    }

    @Override
    @Audited(AuditAction.CONFIRMATION_SAVED)
    public CompleteSolicitationConfirmationResult complete(
            CompleteSolicitationConfirmationCommand command
    ) {
        Objects.requireNonNull(command, "Complete solicitation confirmation command is required");

        Solicitation solicitation = solicitationRepository
                .findById(command.solicitationId())
                .orElseThrow(SolicitationNotFoundException::new);

        if (!solicitation.isOwnedBy(command.clientId())) {
            throw new SolicitationAccessDeniedException();
        }
        if (solicitation.getStatus() != SolicitationStatus.DRAFT) {
            throw new InvalidSolicitationStateException("Only draft solicitations can be edited");
        }
        if (solicitation.getBasicData() == null || solicitation.getAddress() == null) {
            throw new InvalidSolicitationStateException(
                    "Basic data and address must be completed before confirmation"
            );
        }

        Confirmation confirmation = new Confirmation(
                command.priority(),
                command.preferredDate(),
                command.estimatedValue(),
                command.termsAccepted()
        );
        solicitation.completeConfirmation(confirmation, LocalDate.now(clock));

        UUID savedId = solicitationRepository.save(solicitation);
        return CompleteSolicitationConfirmationResult.from(
                savedId,
                solicitation.getStatus(),
                solicitation.getCurrentStep(),
                confirmation
        );
    }
}
