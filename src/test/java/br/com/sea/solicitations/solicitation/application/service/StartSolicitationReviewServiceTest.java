package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.InvalidSolicitationStateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationConcurrentUpdateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationReviewAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewCommand;
import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewResult;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Address;
import br.com.sea.solicitations.solicitation.domain.BasicData;
import br.com.sea.solicitations.solicitation.domain.Confirmation;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartSolicitationReviewServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

    @Mock
    private SolicitationRepository repository;

    private StartSolicitationReviewService service;

    @BeforeEach
    void setUp() {
        service = new StartSolicitationReviewService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void startsReviewForAnalystCoveringTheSolicitationState() {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        Solicitation solicitation = submittedSolicitation();
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));
        when(repository.save(solicitation)).thenReturn(id);

        StartSolicitationReviewResult result = service.start(
                new StartSolicitationReviewCommand(id, analystId, Set.of(FederativeUnit.SP, FederativeUnit.RJ))
        );

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.IN_REVIEW);
        assertThat(solicitation.getAssignedAnalystId()).isEqualTo(analystId);
        assertThat(solicitation.getReviewStartedAt()).isEqualTo(NOW);
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.status()).isEqualTo(SolicitationStatus.IN_REVIEW);
        assertThat(result.assignedAnalystId()).isEqualTo(analystId);
        assertThat(result.reviewStartedAt()).isEqualTo(NOW);
    }

    @Test
    void deniesAnalystOutsideCoverageWithoutChangingSolicitation() {
        UUID id = UUID.randomUUID();
        Solicitation solicitation = submittedSolicitation();
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.start(
                new StartSolicitationReviewCommand(id, UUID.randomUUID(), Set.of(FederativeUnit.RJ))
        )).isInstanceOf(SolicitationReviewAccessDeniedException.class);

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.SUBMITTED);
        assertThat(solicitation.getAssignedAnalystId()).isNull();
        verify(repository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsUnknownSolicitation() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(
                new StartSolicitationReviewCommand(id, UUID.randomUUID(), Set.of(FederativeUnit.SP))
        )).isInstanceOf(SolicitationNotFoundException.class);

        verify(repository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsSecondStartWithoutReassigningAnalyst() {
        UUID id = UUID.randomUUID();
        UUID firstAnalyst = UUID.randomUUID();
        Solicitation solicitation = submittedSolicitation();
        solicitation.startReview(firstAnalyst, NOW.minusSeconds(60));
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.start(
                new StartSolicitationReviewCommand(id, UUID.randomUUID(), Set.of(FederativeUnit.SP))
        )).isInstanceOf(InvalidSolicitationStateException.class);

        assertThat(solicitation.getAssignedAnalystId()).isEqualTo(firstAnalyst);
        verify(repository, never()).save(any(Solicitation.class));
    }

    @Test
    void propagatesOptimisticConcurrencyConflict() {
        UUID id = UUID.randomUUID();
        Solicitation solicitation = submittedSolicitation();
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));
        when(repository.save(solicitation)).thenThrow(new SolicitationConcurrentUpdateException());

        assertThatThrownBy(() -> service.start(
                new StartSolicitationReviewCommand(id, UUID.randomUUID(), Set.of(FederativeUnit.SP))
        )).isInstanceOf(SolicitationConcurrentUpdateException.class);
    }

    private static Solicitation submittedSolicitation() {
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        solicitation.completeBasicData(new BasicData(
                ServiceType.INSTALLATION,
                "Instalacao de equipamento",
                "Solicito a instalacao de um novo equipamento no endereco informado."
        ));
        solicitation.completeAddress(new Address(
                "01001000", "Praca da Se", "Se", "Sao Paulo", FederativeUnit.SP, "100", null
        ));
        solicitation.completeConfirmation(new Confirmation(
                Priority.MEDIUM, TODAY.plusDays(1), null, true
        ), TODAY);
        solicitation.submit(TODAY, NOW.minusSeconds(120));
        return solicitation;
    }
}
