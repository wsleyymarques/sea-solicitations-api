package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.audit.application.annotation.Audited;
import br.com.sea.solicitations.audit.domain.AuditAction;
import br.com.sea.solicitations.solicitation.application.exception.InvalidSolicitationStateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationReviewAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.DecisionOutcome;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Address;
import br.com.sea.solicitations.solicitation.domain.Decision;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public class DecideSolicitationService implements DecideSolicitationUseCase {

    private final SolicitationRepository solicitationRepository;
    private final Clock clock;

    public DecideSolicitationService(SolicitationRepository solicitationRepository, Clock clock) {
        this.solicitationRepository = Objects.requireNonNull(
                solicitationRepository, "Solicitation repository is required"
        );
        this.clock = Objects.requireNonNull(clock, "Clock is required");
    }

    @Override
    @Audited(AuditAction.SOLICITATION_APPROVED)
    public DecideSolicitationResult decide(DecideSolicitationCommand command) {
        Objects.requireNonNull(command, "Decide solicitation command is required");

        Solicitation solicitation = solicitationRepository.findById(command.solicitationId())
                .orElseThrow(SolicitationNotFoundException::new);
        Address address = solicitation.getAddress();
        if (address == null || !command.coveredStates().contains(address.getState())) {
            throw new SolicitationReviewAccessDeniedException();
        }
        if (solicitation.getAssignedAnalystId() == null
                && solicitation.getStatus() != SolicitationStatus.IN_REVIEW) {
            throw new InvalidSolicitationStateException("Only solicitations in review can be decided");
        }
        if (!command.analystId().equals(solicitation.getAssignedAnalystId())) {
            throw new SolicitationReviewAccessDeniedException();
        }
        if (solicitation.getStatus() != SolicitationStatus.IN_REVIEW) {
            throw new InvalidSolicitationStateException("Only solicitations in review can be decided");
        }

        if (command.outcome() == DecisionOutcome.APPROVED) {
            solicitation.approve(command.analystId(), command.comment(), clock.instant());
        } else {
            solicitation.reject(command.analystId(), command.comment(), clock.instant());
        }

        UUID savedId = solicitationRepository.save(solicitation);
        Decision decision = solicitation.getDecision();
        return new DecideSolicitationResult(
                savedId,
                solicitation.getStatus(),
                decision.getAnalystId(),
                decision.getComment(),
                decision.getDecidedAt()
        );
    }
}
