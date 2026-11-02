package br.com.sea.solicitations.solicitation.adapter.out.persistence;

import br.com.sea.solicitations.solicitation.application.exception.SolicitationConcurrentUpdateException;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void translatesOptimisticLockFailureToApplicationConflict() {
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        when(solicitationJpaRepository.save(solicitation))
                .thenThrow(new ObjectOptimisticLockingFailureException(Solicitation.class, UUID.randomUUID()));

        assertThatThrownBy(() -> adapter.save(solicitation))
                .isInstanceOf(SolicitationConcurrentUpdateException.class)
                .hasMessage("Solicitation was updated by another request");
    }
}
