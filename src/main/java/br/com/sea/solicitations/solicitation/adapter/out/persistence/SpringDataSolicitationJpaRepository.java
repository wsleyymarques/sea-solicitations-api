package br.com.sea.solicitations.solicitation.adapter.out.persistence;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.UUID;

public interface SpringDataSolicitationJpaRepository extends JpaRepository<Solicitation, UUID> {

    @Query("""
            SELECT solicitation
            FROM Solicitation solicitation
            WHERE solicitation.address.state IN :coveredStates
              AND (
                    solicitation.status = :submittedStatus
                    OR (
                        solicitation.status = :inReviewStatus
                        AND solicitation.assignedAnalystId = :analystId
                    )
              )
            """)
    Page<Solicitation> findVisibleToAnalyst(
            @Param("coveredStates") Set<FederativeUnit> coveredStates,
            @Param("analystId") UUID analystId,
            @Param("submittedStatus") SolicitationStatus submittedStatus,
            @Param("inReviewStatus") SolicitationStatus inReviewStatus,
            Pageable pageable
    );
}
