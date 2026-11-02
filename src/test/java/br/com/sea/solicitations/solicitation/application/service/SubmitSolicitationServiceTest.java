package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.InvalidSolicitationStateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationConcurrentUpdateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.port.in.SubmitSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.SubmitSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Address;
import br.com.sea.solicitations.solicitation.domain.BasicData;
import br.com.sea.solicitations.solicitation.domain.Confirmation;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;
import br.com.sea.solicitations.solicitation.domain.exception.IncompleteSolicitationException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitSolicitationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);
    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private SolicitationRepository solicitationRepository;

    private SubmitSolicitationService service;

    @BeforeEach
    void setUp() {
        service = new SubmitSolicitationService(solicitationRepository, CLOCK);
    }

    @Test
    void submitsCompleteDraftForItsOwnerUsingThePersistedIdentifier() {
        UUID clientId = UUID.randomUUID();
        UUID requestedId = UUID.randomUUID();
        UUID persistedId = UUID.randomUUID();
        Solicitation solicitation = completeDraft(clientId, TODAY);
        when(solicitationRepository.findById(requestedId)).thenReturn(Optional.of(solicitation));
        when(solicitationRepository.save(solicitation)).thenReturn(persistedId);

        SubmitSolicitationResult result = service.submit(
                new SubmitSolicitationCommand(requestedId, clientId)
        );

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.SUBMITTED);
        assertThat(solicitation.getCurrentStep()).isEqualTo(SolicitationStep.COMPLETED);
        assertThat(solicitation.getSubmittedAt()).isEqualTo(NOW);
        assertThat(result.id()).isEqualTo(persistedId);
        assertThat(result.status()).isEqualTo(SolicitationStatus.SUBMITTED);
        assertThat(result.currentStep()).isEqualTo(SolicitationStep.COMPLETED);
        assertThat(result.submittedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsUnknownSolicitationWithoutSaving() {
        UUID solicitationId = UUID.randomUUID();
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(command(solicitationId, UUID.randomUUID())))
                .isInstanceOf(SolicitationNotFoundException.class)
                .hasMessage("Solicitation was not found");

        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void checksOwnershipBeforeRevealingThatStagesAreMissing() {
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.submit(command(solicitationId, UUID.randomUUID())))
                .isInstanceOf(SolicitationAccessDeniedException.class)
                .hasMessage("Solicitation does not belong to the authenticated client");

        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsSolicitationThatWasAlreadySubmitted() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = completeDraft(clientId, TODAY.plusDays(1));
        solicitation.submit(TODAY, NOW.minusSeconds(60));
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.submit(command(solicitationId, clientId)))
                .isInstanceOf(InvalidSolicitationStateException.class)
                .hasMessage("Only draft solicitations can be submitted");

        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsIncompleteDraftAndPreservesItsState() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = new Solicitation(clientId);
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.submit(command(solicitationId, clientId)))
                .isInstanceOf(IncompleteSolicitationException.class)
                .satisfies(exception -> assertThat(
                        ((IncompleteSolicitationException) exception).missingSteps()
                ).containsExactly(
                        SolicitationStep.BASIC_DATA,
                        SolicitationStep.ADDRESS,
                        SolicitationStep.CONFIRMATION
                ));

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(solicitation.getSubmittedAt()).isNull();
        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsExpiredPreferredDateAsInvalidResourceState() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = completeDraft(clientId, TODAY.minusDays(1));
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.submit(command(solicitationId, clientId)))
                .isInstanceOf(InvalidSolicitationStateException.class)
                .hasMessage("Preferred date cannot be in the past");

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(solicitation.getSubmittedAt()).isNull();
        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void propagatesConcurrentUpdateWithoutChangingTheResultContract() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = completeDraft(clientId, TODAY.plusDays(1));
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));
        when(solicitationRepository.save(solicitation))
                .thenThrow(new SolicitationConcurrentUpdateException());

        assertThatThrownBy(() -> service.submit(command(solicitationId, clientId)))
                .isInstanceOf(SolicitationConcurrentUpdateException.class);
    }

    private static SubmitSolicitationCommand command(UUID solicitationId, UUID clientId) {
        return new SubmitSolicitationCommand(solicitationId, clientId);
    }

    private static Solicitation completeDraft(UUID clientId, LocalDate preferredDate) {
        Solicitation solicitation = new Solicitation(clientId);
        solicitation.completeBasicData(new BasicData(
                ServiceType.INSTALLATION,
                "Instalacao de equipamento",
                "Solicito a instalacao de um novo equipamento no endereco informado."
        ));
        solicitation.completeAddress(new Address(
                "01001000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                FederativeUnit.SP,
                "100",
                null
        ));

        LocalDate confirmationDate = preferredDate.isBefore(TODAY)
                ? preferredDate.minusDays(1)
                : TODAY;
        solicitation.completeConfirmation(new Confirmation(
                Priority.MEDIUM,
                preferredDate,
                null,
                true
        ), confirmationDate);
        return solicitation;
    }
}
