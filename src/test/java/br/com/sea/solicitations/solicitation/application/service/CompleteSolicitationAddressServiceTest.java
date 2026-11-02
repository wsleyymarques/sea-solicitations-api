package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.InvalidSolicitationStateException;
import br.com.sea.solicitations.solicitation.application.exception.PostalCodeServiceUnavailableException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressResult;
import br.com.sea.solicitations.solicitation.application.port.out.PostalCodeData;
import br.com.sea.solicitations.solicitation.application.port.out.PostalCodeLookup;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompleteSolicitationAddressServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    @Mock
    private SolicitationRepository solicitationRepository;

    @Mock
    private PostalCodeLookup postalCodeLookup;

    private CompleteSolicitationAddressService service;

    @BeforeEach
    void setUp() {
        service = new CompleteSolicitationAddressService(
                solicitationRepository,
                postalCodeLookup
        );
    }

    @Test
    void completesAddressWithPostalCodeDataAndAdvancesTheStep() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = draftWithBasicData(clientId);
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));
        when(postalCodeLookup.find("01001000")).thenReturn(new PostalCodeData(
                "01001000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                FederativeUnit.SP
        ));
        when(solicitationRepository.save(solicitation)).thenReturn(solicitationId);

        CompleteSolicitationAddressResult result = service.complete(
                new CompleteSolicitationAddressCommand(
                        solicitationId,
                        clientId,
                        "01001-000",
                        "100",
                        "Apto 10"
                )
        );

        assertThat(solicitation.getAddress()).isEqualTo(new Address(
                "01001000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                FederativeUnit.SP,
                "100",
                "Apto 10"
        ));
        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(solicitation.getCurrentStep()).isEqualTo(SolicitationStep.CONFIRMATION);
        assertThat(result.id()).isEqualTo(solicitationId);
        assertThat(result.status()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(result.currentStep()).isEqualTo(SolicitationStep.CONFIRMATION);
        assertThat(result.postalCode()).isEqualTo("01001000");
        assertThat(result.street()).isEqualTo("Praca da Se");
        assertThat(result.neighborhood()).isEqualTo("Se");
        assertThat(result.city()).isEqualTo("Sao Paulo");
        assertThat(result.state()).isEqualTo(FederativeUnit.SP);
        assertThat(result.number()).isEqualTo("100");
        assertThat(result.complement()).isEqualTo("Apto 10");
    }

    @Test
    void rejectsUnknownSolicitationBeforePostalCodeLookup() {
        UUID solicitationId = UUID.randomUUID();
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(command(solicitationId, UUID.randomUUID())))
                .isInstanceOf(SolicitationNotFoundException.class)
                .hasMessage("Solicitation was not found");

        verifyNoInteractions(postalCodeLookup);
        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsSolicitationOwnedByAnotherClientBeforePostalCodeLookup() {
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = draftWithBasicData(UUID.randomUUID());
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.complete(command(solicitationId, UUID.randomUUID())))
                .isInstanceOf(SolicitationAccessDeniedException.class)
                .hasMessage("Solicitation does not belong to the authenticated client");

        verifyNoInteractions(postalCodeLookup);
        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsSubmittedSolicitationBeforePostalCodeLookup() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = submittedSolicitation(clientId);
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.complete(command(solicitationId, clientId)))
                .isInstanceOf(InvalidSolicitationStateException.class)
                .hasMessage("Only draft solicitations can be edited");

        verifyNoInteractions(postalCodeLookup);
        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void rejectsAddressBeforePostalCodeLookupWhenBasicDataIsMissing() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = new Solicitation(clientId);
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.complete(command(solicitationId, clientId)))
                .isInstanceOf(InvalidSolicitationStateException.class)
                .hasMessage("Basic data must be completed before the address");

        verifyNoInteractions(postalCodeLookup);
        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    @Test
    void doesNotSaveWhenPostalCodeLookupFails() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Solicitation solicitation = draftWithBasicData(clientId);
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));
        when(postalCodeLookup.find("01001000"))
                .thenThrow(new PostalCodeServiceUnavailableException());

        assertThatThrownBy(() -> service.complete(command(solicitationId, clientId)))
                .isInstanceOf(PostalCodeServiceUnavailableException.class);

        assertThat(solicitation.getAddress()).isNull();
        assertThat(solicitation.getCurrentStep()).isEqualTo(SolicitationStep.ADDRESS);
        verify(solicitationRepository, never()).save(any(Solicitation.class));
    }

    private static CompleteSolicitationAddressCommand command(UUID solicitationId, UUID clientId) {
        return new CompleteSolicitationAddressCommand(
                solicitationId,
                clientId,
                "01001-000",
                "100",
                null
        );
    }

    private static Solicitation draftWithBasicData(UUID clientId) {
        Solicitation solicitation = new Solicitation(clientId);
        solicitation.completeBasicData(new BasicData(
                ServiceType.INSTALLATION,
                "Instalacao de equipamento",
                "Solicito a instalacao de um novo equipamento no endereco informado."
        ));
        return solicitation;
    }

    private static Solicitation submittedSolicitation(UUID clientId) {
        Solicitation solicitation = draftWithBasicData(clientId);
        solicitation.completeAddress(new Address(
                "01001000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                FederativeUnit.SP,
                "100",
                null
        ));
        solicitation.completeConfirmation(new Confirmation(
                Priority.MEDIUM,
                TODAY.plusDays(1),
                new BigDecimal("50.00"),
                true
        ), TODAY);
        solicitation.submit(TODAY, Instant.parse("2026-11-01T12:00:00Z"));
        return solicitation;
    }
}
