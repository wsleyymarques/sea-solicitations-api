package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.solicitation.application.port.in.AnalystSolicitationSummary;
import br.com.sea.solicitations.solicitation.application.port.in.ListAnalystSolicitationsQuery;
import br.com.sea.solicitations.solicitation.application.port.in.ListAnalystSolicitationsResult;
import br.com.sea.solicitations.solicitation.application.port.in.ListAnalystSolicitationsUseCase;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationPage;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Solicitation;

import java.util.Objects;

public class ListAnalystSolicitationsService implements ListAnalystSolicitationsUseCase {

    private final SolicitationRepository solicitationRepository;

    public ListAnalystSolicitationsService(SolicitationRepository solicitationRepository) {
        this.solicitationRepository = Objects.requireNonNull(
                solicitationRepository, "Solicitation repository is required"
        );
    }

    @Override
    public ListAnalystSolicitationsResult list(ListAnalystSolicitationsQuery query) {
        Objects.requireNonNull(query, "List analyst solicitations query is required");
        SolicitationPage result = solicitationRepository.findVisibleToAnalyst(
                query.analystId(), query.coveredStates(), query.page(), query.size()
        );

        return new ListAnalystSolicitationsResult(
                result.content().stream().map(ListAnalystSolicitationsService::summary).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    private static AnalystSolicitationSummary summary(Solicitation solicitation) {
        return new AnalystSolicitationSummary(
                solicitation.getId(),
                solicitation.getStatus(),
                solicitation.getBasicData().getServiceType(),
                solicitation.getBasicData().getTitle(),
                solicitation.getConfirmation().getPriority(),
                solicitation.getAddress().getState(),
                solicitation.getSubmittedAt(),
                solicitation.getAssignedAnalystId(),
                solicitation.getReviewStartedAt()
        );
    }
}
