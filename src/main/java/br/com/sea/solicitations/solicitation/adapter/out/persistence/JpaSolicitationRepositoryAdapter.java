package br.com.sea.solicitations.solicitation.adapter.out.persistence;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationConcurrentUpdateException;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationPage;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class JpaSolicitationRepositoryAdapter implements SolicitationRepository {

    private final SpringDataSolicitationJpaRepository solicitationJpaRepository;

    public JpaSolicitationRepositoryAdapter(SpringDataSolicitationJpaRepository solicitationJpaRepository) {
        this.solicitationJpaRepository = Objects.requireNonNull(
                solicitationJpaRepository,
                "Solicitation JPA repository is required"
        );
    }

    @Override
    public Optional<Solicitation> findById(UUID id) {
        return solicitationJpaRepository.findById(
                Objects.requireNonNull(id, "Solicitation ID is required")
        );
    }

    @Override
    public SolicitationPage findVisibleToAnalyst(
            UUID analystId,
            Set<FederativeUnit> coveredStates,
            int page,
            int size
    ) {
        UUID requiredAnalystId = Objects.requireNonNull(analystId, "Analyst ID is required");
        Set<FederativeUnit> requiredCoveredStates = Set.copyOf(
                Objects.requireNonNull(coveredStates, "Covered states are required")
        );
        if (requiredCoveredStates.isEmpty()) {
            return new SolicitationPage(List.of(), page, size, 0, 0);
        }

        Sort sort = Sort.by(
                Sort.Order.desc("submittedAt"),
                Sort.Order.desc("id")
        );
        Page<Solicitation> result = solicitationJpaRepository.findVisibleToAnalyst(
                requiredCoveredStates,
                requiredAnalystId,
                SolicitationStatus.SUBMITTED,
                SolicitationStatus.IN_REVIEW,
                PageRequest.of(page, size, sort)
        );
        return new SolicitationPage(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public UUID save(Solicitation solicitation) {
        try {
            Solicitation savedSolicitation = solicitationJpaRepository.save(
                    Objects.requireNonNull(solicitation, "Solicitation is required")
            );
            return savedSolicitation.getId();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new SolicitationConcurrentUpdateException(exception);
        }
    }
}
