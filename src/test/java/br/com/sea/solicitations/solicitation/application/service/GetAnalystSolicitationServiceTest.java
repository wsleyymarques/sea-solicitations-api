package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationReviewAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.port.in.GetAnalystSolicitationQuery;
import br.com.sea.solicitations.solicitation.application.port.in.GetAnalystSolicitationResult;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAnalystSolicitationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);
    private static final Instant CREATED_AT = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-11-03T11:00:00Z");
    private static final Instant SUBMITTED_AT = Instant.parse("2026-11-02T12:00:00Z");

    @Mock
    private SolicitationRepository repository;

    private GetAnalystSolicitationService service;

    @BeforeEach
    void setUp() {
        service = new GetAnalystSolicitationService(repository);
    }

    @Test
    void returnsCompleteSubmittedSolicitationForCoveredState() {
        UUID id = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Solicitation solicitation = submitted(clientId);
        persisted(solicitation, id);
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));

        GetAnalystSolicitationResult result = service.get(query(id, UUID.randomUUID(), FederativeUnit.SP));

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.clientId()).isEqualTo(clientId);
        assertThat(result.status()).isEqualTo(SolicitationStatus.SUBMITTED);
        assertThat(result.basicData().serviceType()).isEqualTo(ServiceType.INSTALLATION);
        assertThat(result.basicData().title()).isEqualTo("Instalacao de equipamento");
        assertThat(result.basicData().description()).isEqualTo(
                "Solicito a instalacao de um novo equipamento no endereco informado."
        );
        assertThat(result.address().postalCode()).isEqualTo("01001000");
        assertThat(result.address().state()).isEqualTo(FederativeUnit.SP);
        assertThat(result.address().complement()).isEqualTo("Apto 12");
        assertThat(result.confirmation().priority()).isEqualTo(Priority.HIGH);
        assertThat(result.confirmation().estimatedValue()).isEqualByComparingTo("100.00");
        assertThat(result.confirmation().termsAccepted()).isTrue();
        assertThat(result.submittedAt()).isEqualTo(SUBMITTED_AT);
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
        assertThat(result.assignedAnalystId()).isNull();
        assertThat(result.decision()).isNull();
    }

    @Test
    void deniesSubmittedSolicitationThatAlreadyHasAnAssignedAnalyst() {
        UUID id = UUID.randomUUID();
        Solicitation solicitation = submitted(UUID.randomUUID());
        ReflectionTestUtils.setField(solicitation, "assignedAnalystId", UUID.randomUUID());
        persisted(solicitation, id);
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.get(query(id, UUID.randomUUID(), FederativeUnit.SP)))
                .isInstanceOf(SolicitationReviewAccessDeniedException.class);
    }

    @Test
    void returnsTerminalSolicitationOnlyToAssignedAnalyst() {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        Solicitation solicitation = submitted(UUID.randomUUID());
        solicitation.startReview(analystId, SUBMITTED_AT.plusSeconds(60));
        solicitation.approve(analystId, "Parecer favoravel para atendimento.", SUBMITTED_AT.plusSeconds(120));
        persisted(solicitation, id);
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));

        GetAnalystSolicitationResult result = service.get(query(id, analystId, FederativeUnit.SP));

        assertThat(result.status()).isEqualTo(SolicitationStatus.APPROVED);
        assertThat(result.assignedAnalystId()).isEqualTo(analystId);
        assertThat(result.decision().analystId()).isEqualTo(analystId);
        assertThat(result.decision().comment()).isEqualTo("Parecer favoravel para atendimento.");
        assertThat(result.decision().decidedAt()).isEqualTo(SUBMITTED_AT.plusSeconds(120));
    }

    @Test
    void rejectsUnknownSolicitation() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(query(id, UUID.randomUUID(), FederativeUnit.SP)))
                .isInstanceOf(SolicitationNotFoundException.class);
    }

    @Test
    void deniesDraftAndStateOutsideCoverage() {
        UUID draftId = UUID.randomUUID();
        Solicitation draft = new Solicitation(UUID.randomUUID());
        draft.completeBasicData(new BasicData(
                ServiceType.INSTALLATION,
                "Instalacao de equipamento",
                "Solicito a instalacao de um novo equipamento no endereco informado."
        ));
        when(repository.findById(draftId)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.get(query(draftId, UUID.randomUUID(), FederativeUnit.SP)))
                .isInstanceOf(SolicitationReviewAccessDeniedException.class);

        UUID submittedId = UUID.randomUUID();
        when(repository.findById(submittedId)).thenReturn(Optional.of(submitted(UUID.randomUUID())));

        assertThatThrownBy(() -> service.get(query(submittedId, UUID.randomUUID(), FederativeUnit.RJ)))
                .isInstanceOf(SolicitationReviewAccessDeniedException.class);
    }

    @Test
    void deniesSolicitationAssignedToAnotherAnalyst() {
        UUID id = UUID.randomUUID();
        Solicitation solicitation = submitted(UUID.randomUUID());
        solicitation.startReview(UUID.randomUUID(), SUBMITTED_AT.plusSeconds(60));
        when(repository.findById(id)).thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> service.get(query(id, UUID.randomUUID(), FederativeUnit.SP)))
                .isInstanceOf(SolicitationReviewAccessDeniedException.class);
    }

    private static GetAnalystSolicitationQuery query(
            UUID solicitationId, UUID analystId, FederativeUnit coveredState
    ) {
        return new GetAnalystSolicitationQuery(solicitationId, analystId, Set.of(coveredState));
    }

    private static Solicitation submitted(UUID clientId) {
        Solicitation solicitation = new Solicitation(clientId);
        solicitation.completeBasicData(new BasicData(
                ServiceType.INSTALLATION,
                "Instalacao de equipamento",
                "Solicito a instalacao de um novo equipamento no endereco informado."
        ));
        solicitation.completeAddress(new Address(
                "01001-000", "Praca da Se", "Se", "Sao Paulo", FederativeUnit.SP,
                "100", "Apto 12"
        ));
        solicitation.completeConfirmation(new Confirmation(
                Priority.HIGH, TODAY.plusDays(1), new BigDecimal("100.00"), true
        ), TODAY);
        solicitation.submit(TODAY, SUBMITTED_AT);
        return solicitation;
    }

    private static void persisted(Solicitation solicitation, UUID id) {
        ReflectionTestUtils.setField(solicitation, "id", id);
        ReflectionTestUtils.setField(solicitation, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(solicitation, "updatedAt", UPDATED_AT);
    }
}
