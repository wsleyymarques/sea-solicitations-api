package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationReviewAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.port.in.GetAnalystSolicitationQuery;
import br.com.sea.solicitations.solicitation.application.port.in.GetAnalystSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.in.GetAnalystSolicitationUseCase;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.util.Objects;

public class GetAnalystSolicitationService implements GetAnalystSolicitationUseCase {

    private final SolicitationRepository repository;

    public GetAnalystSolicitationService(SolicitationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Solicitation repository is required");
    }

    @Override
    public GetAnalystSolicitationResult get(GetAnalystSolicitationQuery query) {
        Objects.requireNonNull(query, "Get analyst solicitation query is required");
        Solicitation solicitation = repository.findById(query.solicitationId())
                .orElseThrow(SolicitationNotFoundException::new);

        if (!isVisible(query, solicitation)) {
            throw new SolicitationReviewAccessDeniedException();
        }
        return GetAnalystSolicitationResult.from(solicitation);
    }

    private static boolean isVisible(GetAnalystSolicitationQuery query, Solicitation solicitation) {
        if (solicitation.getAddress() == null
                || !query.coveredStates().contains(solicitation.getAddress().getState())) {
            return false;
        }
        if (solicitation.getStatus() == SolicitationStatus.SUBMITTED) {
            return solicitation.getAssignedAnalystId() == null;
        }
        return (solicitation.getStatus() == SolicitationStatus.IN_REVIEW
                || solicitation.getStatus() == SolicitationStatus.APPROVED
                || solicitation.getStatus() == SolicitationStatus.REJECTED)
                && query.analystId().equals(solicitation.getAssignedAnalystId());
    }
}
