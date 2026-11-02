package br.com.sea.solicitations.solicitation.adapter.out.persistence;

import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationConcurrentUpdateException;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;
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
