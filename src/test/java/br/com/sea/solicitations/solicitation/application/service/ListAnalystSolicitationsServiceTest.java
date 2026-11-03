package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.port.in.ListAnalystSolicitationsQuery;
import br.com.sea.solicitations.solicitation.application.port.in.ListAnalystSolicitationsResult;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationPage;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAnalystSolicitationsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);
    private static final Instant SUBMITTED_AT = Instant.parse("2026-11-02T12:00:00Z");

    @Mock
    private SolicitationRepository repository;

    private ListAnalystSolicitationsService service;

    @BeforeEach
    void setUp() {
        service = new ListAnalystSolicitationsService(repository);
    }

    @Test
    void returnsMappedPageVisibleToAuthenticatedAnalyst() {
        UUID analystId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        Set<FederativeUnit> coverage = Set.of(FederativeUnit.SP, FederativeUnit.RJ);
        Solicitation solicitation = submittedSolicitation(solicitationId);
        when(repository.findVisibleToAnalyst(analystId, coverage, 2, 5))
                .thenReturn(new SolicitationPage(List.of(solicitation), 2, 5, 11, 3));

        ListAnalystSolicitationsResult result = service.list(
                new ListAnalystSolicitationsQuery(analystId, coverage, 2, 5)
        );

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(11);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.content()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(solicitationId);
            assertThat(item.status()).isEqualTo(SolicitationStatus.SUBMITTED);
            assertThat(item.serviceType()).isEqualTo(ServiceType.INSTALLATION);
            assertThat(item.title()).isEqualTo("Instalacao de equipamento");
            assertThat(item.priority()).isEqualTo(Priority.MEDIUM);
            assertThat(item.state()).isEqualTo(FederativeUnit.SP);
            assertThat(item.submittedAt()).isEqualTo(SUBMITTED_AT);
            assertThat(item.assignedAnalystId()).isNull();
            assertThat(item.reviewStartedAt()).isNull();
        });
    }

    @Test
    void acceptsEmptyCoverageAsNoVisibleSolicitations() {
        UUID analystId = UUID.randomUUID();
        when(repository.findVisibleToAnalyst(analystId, Set.of(), 0, 20))
                .thenReturn(new SolicitationPage(List.of(), 0, 20, 0, 0));

        ListAnalystSolicitationsResult result = service.list(
                new ListAnalystSolicitationsQuery(analystId, Set.of(), 0, 20)
        );

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void rejectsInvalidPagination() {
        UUID analystId = UUID.randomUUID();
        Set<FederativeUnit> coverage = Set.of(FederativeUnit.SP);

        assertThatThrownBy(() -> new ListAnalystSolicitationsQuery(analystId, coverage, -1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page must be zero or greater");
        assertThatThrownBy(() -> new ListAnalystSolicitationsQuery(analystId, coverage, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Size must be between 1 and 100");
        assertThatThrownBy(() -> new ListAnalystSolicitationsQuery(analystId, coverage, 0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Size must be between 1 and 100");
    }

    private static Solicitation submittedSolicitation(UUID id) {
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
        solicitation.submit(TODAY, SUBMITTED_AT);
        ReflectionTestUtils.setField(solicitation, "id", id);
        return solicitation;
    }
}
