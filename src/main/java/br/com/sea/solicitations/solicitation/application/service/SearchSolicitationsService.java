package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsQuery;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsResult;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationSummary;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationSearchIndex;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SearchSolicitationsService implements SearchSolicitationsUseCase {

    private final SolicitationSearchIndex index;
    private final SolicitationRepository repository;

    public SearchSolicitationsService(SolicitationSearchIndex index, SolicitationRepository repository) {
        this.index = Objects.requireNonNull(index, "Search index is required");
        this.repository = Objects.requireNonNull(repository, "Solicitation repository is required");
    }

    @Override
    public SearchSolicitationsResult search(SearchSolicitationsQuery query) {
        Objects.requireNonNull(query, "Search query is required");
        if (query.coveredStates().isEmpty()
                || (query.state() != null && !query.coveredStates().contains(query.state()))) {
            return new SearchSolicitationsResult(List.of(), query.page(), query.size(), 0, 0);
        }
        SearchSolicitationsResult result = index.search(query);
        if (result.content().isEmpty()) {
            return result;
        }
        List<SearchSolicitationSummary> visible = result.content().stream()
                .map(hit -> repository.findById(hit.id())
                        .filter(solicitation -> visibleTo(query, solicitation))
                        .map(solicitation -> summary(hit.id(), solicitation))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
        return new SearchSolicitationsResult(
                visible, result.page(), result.size(), result.totalElements(), result.totalPages()
        );
    }

    private static boolean visibleTo(SearchSolicitationsQuery query, Solicitation solicitation) {
        if (solicitation.getAddress() == null
                || !query.coveredStates().contains(solicitation.getAddress().getState())) {
            return false;
        }
        SolicitationStatus status = solicitation.getStatus();
        if (query.status() != null && query.status() != status) {
            return false;
        }
        if (status == SolicitationStatus.SUBMITTED) {
            return true;
        }
        return (status == SolicitationStatus.IN_REVIEW
                || status == SolicitationStatus.APPROVED
                || status == SolicitationStatus.REJECTED)
                && query.analystId().equals(solicitation.getAssignedAnalystId());
    }

    private static SearchSolicitationSummary summary(UUID id, Solicitation solicitation) {
        return new SearchSolicitationSummary(
                id,
                solicitation.getStatus(),
                solicitation.getBasicData().getServiceType(),
                solicitation.getBasicData().getTitle(),
                solicitation.getConfirmation().getPriority(),
                solicitation.getAddress().getState(),
                solicitation.getCreatedAt(),
                solicitation.getSubmittedAt(),
                solicitation.getConfirmation().getPreferredDate(),
                solicitation.getAssignedAnalystId()
        );
    }
}
