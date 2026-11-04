package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.InvalidSolicitationStateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationConcurrentUpdateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationReviewAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.in.DecisionOutcome;
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
class DecideSolicitationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T12:00:00Z");
    private static final String COMMENT = "  Parecer favoravel para atendimento.  ";

    @Mock
    private SolicitationRepository repository;

    private DecideSolicitationService service;

    @BeforeEach
    void setUp() {
        service = new DecideSolicitationService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void approvesAndPersistsDecisionWithNormalizedComment() {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        Solicitation solicitation = inReview(analystId);
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));
        when(repository.save(solicitation)).thenReturn(id);

        DecideSolicitationResult result = service.decide(command(id, analystId, DecisionOutcome.APPROVE, COMMENT));

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.status()).isEqualTo(SolicitationStatus.APPROVED);
        assertThat(result.analystId()).isEqualTo(analystId);
        assertThat(result.comment()).isEqualTo("Parecer favoravel para atendimento.");
        assertThat(result.decidedAt()).isEqualTo(NOW);
        assertThat(solicitation.getDecision().getComment()).isEqualTo(result.comment());
        verify(repository).save(solicitation);
    }

    @Test
    void rejectsAndPersistsDecision() {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        Solicitation solicitation = inReview(analystId);
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));
        when(repository.save(solicitation)).thenReturn(id);

        DecideSolicitationResult result = service.decide(command(
                id, analystId, DecisionOutcome.REJECT, "Documentacao insuficiente."
        ));

        assertThat(result.status()).isEqualTo(SolicitationStatus.REJECTED);
        assertThat(solicitation.getDecision().getAnalystId()).isEqualTo(analystId);
        assertThat(solicitation.getDecision().getDecidedAt()).isEqualTo(NOW);
    }

    @Test
    void deniesAnalystOutsideCoverageBeforeMutation() {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        Solicitation solicitation = inReview(analystId);
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.decide(new DecideSolicitationCommand(
                id, analystId, Set.of(FederativeUnit.RJ), DecisionOutcome.APPROVE, COMMENT
        ))).isInstanceOf(SolicitationReviewAccessDeniedException.class);

        assertThat(solicitation.getDecision()).isNull();
        verify(repository, never()).save(any(Solicitation.class));
    }

    @Test
    void deniesAnalystOtherThanAssigned() {
        UUID id = UUID.randomUUID();
        Solicitation solicitation = inReview(UUID.randomUUID());
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.decide(command(
                id, UUID.randomUUID(), DecisionOutcome.APPROVE, COMMENT
        ))).isInstanceOf(SolicitationReviewAccessDeniedException.class);

        assertThat(solicitation.getDecision()).isNull();
        verify(repository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsSecondDecisionWithoutReplacingFirst() {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        Solicitation solicitation = inReview(analystId);
        solicitation.approve(analystId, "Primeira decisao favoravel.", NOW.minusSeconds(30));
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.decide(command(
                id, analystId, DecisionOutcome.REJECT, COMMENT
        ))).isInstanceOf(InvalidSolicitationStateException.class);

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.APPROVED);
        assertThat(solicitation.getDecision().getComment()).isEqualTo("Primeira decisao favoravel.");
        verify(repository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsSubmittedSolicitation() {
        UUID id = UUID.randomUUID();
        Solicitation solicitation = submitted();
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.decide(command(
                id, UUID.randomUUID(), DecisionOutcome.APPROVE, COMMENT
        ))).isInstanceOf(InvalidSolicitationStateException.class);
        verify(repository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsCommentBelowMinimumAfterTrimming() {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        Solicitation solicitation = inReview(analystId);
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.decide(command(
                id, analystId, DecisionOutcome.APPROVE, "   curto   "
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.IN_REVIEW);
        assertThat(solicitation.getDecision()).isNull();
        verify(repository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsBlankMissingAndOverlongCommentsWithoutSaving() {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        Solicitation solicitation = inReview(analystId);
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));

        for (String comment : new String[]{null, "      ", "x".repeat(1001)}) {
            assertThatThrownBy(() -> service.decide(command(
                    id, analystId, DecisionOutcome.REJECT, comment
            ))).isInstanceOf(IllegalArgumentException.class);
        }

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.IN_REVIEW);
        assertThat(solicitation.getDecision()).isNull();
        verify(repository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsUnknownSolicitation() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.decide(command(
                id, UUID.randomUUID(), DecisionOutcome.APPROVE, COMMENT
        ))).isInstanceOf(SolicitationNotFoundException.class);
    }

    @Test
    void propagatesOptimisticLockConflict() {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        Solicitation solicitation = inReview(analystId);
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));
        when(repository.save(solicitation)).thenThrow(new SolicitationConcurrentUpdateException());

        assertThatThrownBy(() -> service.decide(command(
                id, analystId, DecisionOutcome.APPROVE, COMMENT
        ))).isInstanceOf(SolicitationConcurrentUpdateException.class);
    }

    private static DecideSolicitationCommand command(UUID id, UUID analystId, DecisionOutcome outcome, String comment) {
        return new DecideSolicitationCommand(id, analystId, Set.of(FederativeUnit.SP), outcome, comment);
    }

    private static Solicitation inReview(UUID analystId) {
        Solicitation solicitation = submitted();
        solicitation.startReview(analystId, NOW.minusSeconds(60));
        return solicitation;
    }

    private static Solicitation submitted() {
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        solicitation.completeBasicData(new BasicData(
                ServiceType.INSTALLATION, "Instalacao de equipamento",
                "Solicito a instalacao de um novo equipamento no endereco informado."
        ));
        solicitation.completeAddress(new Address(
                "01001000", "Praca da Se", "Se", "Sao Paulo", FederativeUnit.SP, "100", null
        ));
        solicitation.completeConfirmation(new Confirmation(
                Priority.MEDIUM, LocalDate.of(2026, 11, 3), null, true
        ), LocalDate.of(2026, 11, 2));
        solicitation.submit(LocalDate.of(2026, 11, 2), NOW.minusSeconds(120));
        return solicitation;
    }
}
