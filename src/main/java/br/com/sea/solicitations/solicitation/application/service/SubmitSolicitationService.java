package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.audit.application.annotation.Audited;
import br.com.sea.solicitations.audit.domain.AuditAction;
import br.com.sea.solicitations.solicitation.application.exception.InvalidSolicitationStateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.port.in.SubmitSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.SubmitSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.in.SubmitSolicitationUseCase;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class SubmitSolicitationService implements SubmitSolicitationUseCase {

    private final SolicitationRepository solicitationRepository;
    private final Clock clock;

    public SubmitSolicitationService(
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
    @Audited(AuditAction.SOLICITATION_SUBMITTED)
    public SubmitSolicitationResult submit(SubmitSolicitationCommand command) {
        Objects.requireNonNull(command, "Submit solicitation command is required");

        Solicitation solicitation = solicitationRepository
                .findById(command.solicitationId())
                .orElseThrow(SolicitationNotFoundException::new);

        if (!solicitation.isOwnedBy(command.clientId())) {
            throw new SolicitationAccessDeniedException();
        }
        if (solicitation.getStatus() != SolicitationStatus.DRAFT) {
            throw new InvalidSolicitationStateException(
                    "Only draft solicitations can be submitted"
            );
        }

        Instant submittedAt = clock.instant();
        LocalDate currentDate = LocalDate.ofInstant(submittedAt, clock.getZone());
        try {
            solicitation.submit(currentDate, submittedAt);
        } catch (IllegalArgumentException exception) {
            throw new InvalidSolicitationStateException(exception.getMessage());
        }

        UUID savedId = solicitationRepository.save(solicitation);
        return new SubmitSolicitationResult(
                savedId,
                solicitation.getStatus(),
                solicitation.getCurrentStep(),
                solicitation.getSubmittedAt()
        );
    }
}
