package br.com.sea.solicitations.solicitation.adapter.out.persistence;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationConcurrentUpdateException;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationPage;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaSolicitationRepositoryAdapterTest {

    @Mock
    private SpringDataSolicitationJpaRepository solicitationJpaRepository;

    private JpaSolicitationRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaSolicitationRepositoryAdapter(solicitationJpaRepository);
    }

    @Test
    void loadsSolicitationById() {
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        when(solicitationJpaRepository.findById(solicitationId))
                .thenReturn(Optional.of(solicitation));

        Optional<Solicitation> result = adapter.findById(solicitationId);

        assertThat(result).containsSame(solicitation);
        verify(solicitationJpaRepository).findById(solicitationId);
    }

    @Test
    void queriesVisibleSolicitationsWithStableNewestFirstOrdering() {
        UUID analystId = UUID.randomUUID();
        Set<FederativeUnit> coverage = Set.of(FederativeUnit.SP, FederativeUnit.RJ);
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        when(solicitationJpaRepository.findVisibleToAnalyst(
                eq(coverage),
                eq(analystId),
                eq(SolicitationStatus.SUBMITTED),
                eq(SolicitationStatus.IN_REVIEW),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(solicitation)));

        SolicitationPage result = adapter.findVisibleToAnalyst(analystId, coverage, 1, 10);

        assertThat(result.content()).containsExactly(solicitation);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(solicitationJpaRepository).findVisibleToAnalyst(
                eq(coverage),
                eq(analystId),
                eq(SolicitationStatus.SUBMITTED),
                eq(SolicitationStatus.IN_REVIEW),
                pageableCaptor.capture()
        );
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("submittedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void returnsEmptyPageWithoutQueryWhenCoverageIsEmpty() {
        SolicitationPage result = adapter.findVisibleToAnalyst(UUID.randomUUID(), Set.of(), 3, 25);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(3);
        assertThat(result.size()).isEqualTo(25);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        verifyNoInteractions(solicitationJpaRepository);
    }

    @Test
    void translatesOptimisticLockFailureToApplicationConflict() {
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        when(solicitationJpaRepository.save(solicitation))
                .thenThrow(new ObjectOptimisticLockingFailureException(Solicitation.class, UUID.randomUUID()));

        assertThatThrownBy(() -> adapter.save(solicitation))
                .isInstanceOf(SolicitationConcurrentUpdateException.class)
                .hasMessage("Solicitation was updated by another request");
    }
}
