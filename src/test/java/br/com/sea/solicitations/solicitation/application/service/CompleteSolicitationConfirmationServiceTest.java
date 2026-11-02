package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.InvalidSolicitationStateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationConfirmationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationConfirmationResult;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Address;
import br.com.sea.solicitations.solicitation.domain.BasicData;
import br.com.sea.solicitations.solicitation.domain.Confirmation;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class CompleteSolicitationConfirmationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-01T12:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private SolicitationRepository solicitationRepository;

    private CompleteSolicitationConfirmationService service;

    @BeforeEach
    void setUp() {
        service = new CompleteSolicitationConfirmationService(solicitationRepository, CLOCK);
    }

    @Test
    void completesConfirmationForTodayAndKeepsTheSolicitationAsDraft() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = draftWithAddress(clientId);
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));
        when(solicitationRepository.save(solicitation)).thenReturn(solicitationId);

        CompleteSolicitationConfirmationResult result = service.complete(new CompleteSolicitationConfirmationCommand(
                solicitationId,
                clientId,
                Priority.HIGH,
                TODAY,
                new BigDecimal("100.00"),
                true
        ));

        assertThat(solicitation.getConfirmation()).isEqualTo(new Confirmation(
                Priority.HIGH,
                TODAY,
                new BigDecimal("100.00"),
                true
        ));
        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(solicitation.getCurrentStep()).isEqualTo(SolicitationStep.COMPLETED);
        assertThat(result.id()).isEqualTo(solicitationId);
        assertThat(result.status()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(result.currentStep()).isEqualTo(SolicitationStep.COMPLETED);
        assertThat(result.priority()).isEqualTo(Priority.HIGH);
        assertThat(result.preferredDate()).isEqualTo(TODAY);
        assertThat(result.estimatedValue()).isEqualByComparingTo("100.00");
        assertThat(result.termsAccepted()).isTrue();
    }

    @Test
    void replacesConfirmationWhileCompletedSolicitationIsStillDraft() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = draftWithAddress(clientId);
        solicitation.completeConfirmation(new Confirmation(
                Priority.MEDIUM,
                TODAY.plusDays(1),
                new BigDecimal("50.00"),
                true
        ), TODAY);
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));
        when(solicitationRepository.save(solicitation)).thenReturn(solicitationId);

        CompleteSolicitationConfirmationResult result = service.complete(new CompleteSolicitationConfirmationCommand(
                solicitationId,
                clientId,
                Priority.LOW,
                TODAY.plusDays(2),
                null,
                true
        ));

        assertThat(solicitation.getConfirmation().getPriority()).isEqualTo(Priority.LOW);
        assertThat(solicitation.getConfirmation().getEstimatedValue()).isNull();
        assertThat(solicitation.getCurrentStep()).isEqualTo(SolicitationStep.COMPLETED);
        assertThat(result.estimatedValue()).isNull();
    }

    @Test
    void completesMediumPriorityWithoutEstimatedValue() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = draftWithAddress(clientId);
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));
        when(solicitationRepository.save(solicitation)).thenReturn(solicitationId);

        CompleteSolicitationConfirmationResult result = service.complete(
                command(solicitationId, clientId)
        );

        assertThat(result.priority()).isEqualTo(Priority.MEDIUM);
        assertThat(result.estimatedValue()).isNull();
        assertThat(solicitation.getCurrentStep()).isEqualTo(SolicitationStep.COMPLETED);
    }

    @Test
    void rejectsUnknownSolicitation() {
        UUID solicitationId = UUID.randomUUID();
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(command(solicitationId, UUID.randomUUID())))
                .isInstanceOf(SolicitationNotFoundException.class)
                .hasMessage("Solicitation was not found");

        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsSolicitationOwnedByAnotherClient() {
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = draftWithAddress(UUID.randomUUID());
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.complete(command(solicitationId, UUID.randomUUID())))
                .isInstanceOf(SolicitationAccessDeniedException.class)
                .hasMessage("Solicitation does not belong to the authenticated client");

        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsSolicitationThatIsNoLongerDraft() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = submittedSolicitation(clientId);
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.complete(command(solicitationId, clientId)))
                .isInstanceOf(InvalidSolicitationStateException.class)
                .hasMessage("Only draft solicitations can be edited");

        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsConfirmationWhenPreviousStepsAreIncomplete() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = new Solicitation(clientId);
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.complete(command(solicitationId, clientId)))
                .isInstanceOf(InvalidSolicitationStateException.class)
                .hasMessage("Basic data and address must be completed before confirmation");

        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsPreferredDateBeforeTodayWithoutChangingTheAggregate() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = draftWithAddress(clientId);
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.complete(new CompleteSolicitationConfirmationCommand(
                solicitationId,
                clientId,
                Priority.MEDIUM,
                TODAY.minusDays(1),
                null,
                true
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Preferred date cannot be in the past");

        assertThat(solicitation.getConfirmation()).isNull();
        assertThat(solicitation.getCurrentStep()).isEqualTo(SolicitationStep.CONFIRMATION);
        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsHighPriorityWithoutTheMinimumEstimatedValue() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = draftWithAddress(clientId);
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.complete(new CompleteSolicitationConfirmationCommand(
                solicitationId,
                clientId,
                Priority.HIGH,
                TODAY.plusDays(1),
                new BigDecimal("99.99"),
                true
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("High priority requires an estimated value of at least 100.00");

        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsHighPriorityWithoutEstimatedValue() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = draftWithAddress(clientId);
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.complete(new CompleteSolicitationConfirmationCommand(
                solicitationId,
                clientId,
                Priority.HIGH,
                TODAY.plusDays(1),
                null,
                true
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("High priority requires an estimated value of at least 100.00");

        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    private static CompleteSolicitationConfirmationCommand command(UUID solicitationId, UUID clientId) {
        return new CompleteSolicitationConfirmationCommand(
                solicitationId,
                clientId,
                Priority.MEDIUM,
                TODAY.plusDays(1),
                null,
                true
        );
    }

    private static Solicitation draftWithAddress(UUID clientId) {
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
        return solicitation;
    }

    private static Solicitation submittedSolicitation(UUID clientId) {
        Solicitation solicitation = draftWithAddress(clientId);
        solicitation.completeConfirmation(new Confirmation(
                Priority.MEDIUM,
                TODAY.plusDays(1),
                null,
                true
        ), TODAY);
        solicitation.submit(TODAY, Instant.parse("2026-11-01T12:00:00Z"));
        return solicitation;
    }
}
