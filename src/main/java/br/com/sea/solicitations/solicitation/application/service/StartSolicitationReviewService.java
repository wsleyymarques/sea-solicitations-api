package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.audit.application.annotation.Audited;
import br.com.sea.solicitations.audit.domain.AuditAction;
import br.com.sea.solicitations.solicitation.application.exception.InvalidSolicitationStateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationReviewAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewCommand;
import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewResult;
import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewUseCase;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Address;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public class StartSolicitationReviewService implements StartSolicitationReviewUseCase {

    private final SolicitationRepository solicitationRepository;
    private final Clock clock;

    public StartSolicitationReviewService(SolicitationRepository solicitationRepository, Clock clock) {
        this.solicitationRepository = Objects.requireNonNull(
                solicitationRepository, "Solicitation repository is required"
        );
        this.clock = Objects.requireNonNull(clock, "Clock is required");
    }

    @Override
    @Audited(AuditAction.REVIEW_STARTED)
    public StartSolicitationReviewResult start(StartSolicitationReviewCommand command) {
        Objects.requireNonNull(command, "Start solicitation review command is required");

        Solicitation solicitation = solicitationRepository.findById(command.solicitationId())
                .orElseThrow(SolicitationNotFoundException::new);

        Address address = solicitation.getAddress();
        if (address == null || !command.coveredStates().contains(address.getState())) {
            throw new SolicitationReviewAccessDeniedException();
        }
        if (solicitation.getStatus() != SolicitationStatus.SUBMITTED) {
            throw new InvalidSolicitationStateException("Only submitted solicitations can start review");
        }

        solicitation.startReview(command.analystId(), clock.instant());
        UUID savedId = solicitationRepository.save(solicitation);
        return new StartSolicitationReviewResult(
                savedId,
                solicitation.getStatus(),
                solicitation.getAssignedAnalystId(),
                solicitation.getReviewStartedAt()
        );
    }
}
