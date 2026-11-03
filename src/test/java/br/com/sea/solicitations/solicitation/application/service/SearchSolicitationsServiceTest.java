package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.port.in.SearchDirection;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsQuery;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsResult;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationSummary;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSortField;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationSearchIndex;
import br.com.sea.solicitations.solicitation.domain.Address;
import br.com.sea.solicitations.solicitation.domain.BasicData;
import br.com.sea.solicitations.solicitation.domain.Confirmation;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SearchSolicitationsServiceTest {

    private final SolicitationSearchIndex index = mock(SolicitationSearchIndex.class);
    private final SolicitationRepository repository = mock(SolicitationRepository.class);
    private final SearchSolicitationsService service = new SearchSolicitationsService(index, repository);

    @Test
    void delegatesSearchWithIdentityAndCoverage() {
        SearchSolicitationsQuery query = query(Set.of(FederativeUnit.SP), FederativeUnit.SP);
        SearchSolicitationsResult page = new SearchSolicitationsResult(List.of(), 0, 20, 0, 0);
        when(index.search(query)).thenReturn(page);

        assertThat(service.search(query)).isSameAs(page);
        verify(index).search(query);
    }

    @Test
    void skipsSearchForEmptyCoverageOrStateOutsideCoverage() {
        SearchSolicitationsResult noCoverage = service.search(query(Set.of(), null));
        SearchSolicitationsResult outsideCoverage = service.search(query(
                Set.of(FederativeUnit.SP), FederativeUnit.RJ
        ));

        assertThat(noCoverage.content()).isEmpty();
        assertThat(outsideCoverage.content()).isEmpty();
        assertThat(outsideCoverage.page()).isZero();
        assertThat(outsideCoverage.size()).isEqualTo(20);
        verifyNoInteractions(index);
    }

    @Test
    void rejectsInvalidPaginationDatesAndBlankLongText() {
        assertThatThrownBy(() -> query(Set.of(FederativeUnit.SP), null, -1, 20,
                null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> query(Set.of(FederativeUnit.SP), null, 0, 101,
                null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> query(Set.of(FederativeUnit.SP), null, 500, 20,
                null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> query(Set.of(FederativeUnit.SP), null, 0, 20,
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 2), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> query(Set.of(FederativeUnit.SP), null, 0, 20,
                null, null, "x".repeat(201))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizesWhitespaceOnlyTextToNoTextFilter() {
        assertThat(query(Set.of(FederativeUnit.SP), null, 0, 20,
                null, null, "   ").text()).isNull();
    }

    @Test
    void excludesStaleSubmittedHitAlreadyAssignedToAnotherAnalystInDatabase() {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        SearchSolicitationsQuery query = new SearchSolicitationsQuery(
                analystId, Set.of(FederativeUnit.SP), null, null, null, null, null,
                null, null, 0, 20, SearchSortField.SUBMITTED_AT, SearchDirection.DESC
        );
        when(index.search(query)).thenReturn(new SearchSolicitationsResult(List.of(
                new SearchSolicitationSummary(
                        id, SolicitationStatus.SUBMITTED, ServiceType.INSTALLATION,
                        "Instalacao de equipamento", Priority.MEDIUM, FederativeUnit.SP,
                        Instant.parse("2026-11-02T10:00:00Z"), Instant.parse("2026-11-02T12:00:00Z"),
                        LocalDate.of(2026, 11, 3), null
                )
        ), 0, 20, 1, 1));
        Solicitation actual = submittedSolicitation();
        actual.startReview(UUID.randomUUID(), Instant.parse("2026-11-02T12:01:00Z"));
        when(repository.findById(id)).thenReturn(Optional.of(actual));

        assertThat(service.search(query).content()).isEmpty();
    }

    @Test
    void returnsCurrentTerminalStateForAssignedAnalyst() {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        SearchSolicitationsQuery query = new SearchSolicitationsQuery(
                analystId, Set.of(FederativeUnit.SP), null, null, null, null, null,
                null, null, 0, 20, SearchSortField.SUBMITTED_AT, SearchDirection.DESC
        );
        when(index.search(query)).thenReturn(new SearchSolicitationsResult(List.of(
                new SearchSolicitationSummary(
                        id, SolicitationStatus.SUBMITTED, ServiceType.INSTALLATION,
                        "Instalacao de equipamento", Priority.MEDIUM, FederativeUnit.SP,
                        Instant.parse("2026-11-02T10:00:00Z"), Instant.parse("2026-11-02T12:00:00Z"),
                        LocalDate.of(2026, 11, 3), null
                )
        ), 0, 20, 1, 1));
        Solicitation actual = submittedSolicitation();
        actual.startReview(analystId, Instant.parse("2026-11-02T12:01:00Z"));
        actual.approve(analystId, "Parecer favoravel para atendimento.",
                Instant.parse("2026-11-02T12:02:00Z"));
        when(repository.findById(id)).thenReturn(Optional.of(actual));

        var result = service.search(query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().status()).isEqualTo(SolicitationStatus.APPROVED);
        assertThat(result.content().getFirst().assignedAnalystId()).isEqualTo(analystId);
    }

    private static Solicitation submittedSolicitation() {
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        solicitation.completeBasicData(new BasicData(
                ServiceType.INSTALLATION, "Instalacao de equipamento",
                "Solicito instalacao de equipamento no endereco indicado."
        ));
        solicitation.completeAddress(new Address(
                "01001000", "Praca da Se", "Se", "Sao Paulo", FederativeUnit.SP, "100", null
        ));
        solicitation.completeConfirmation(new Confirmation(
                Priority.MEDIUM, LocalDate.of(2026, 11, 3), null, true
        ), LocalDate.of(2026, 11, 2));
        solicitation.submit(LocalDate.of(2026, 11, 2), Instant.parse("2026-11-02T12:00:00Z"));
        return solicitation;
    }

    private static SearchSolicitationsQuery query(Set<FederativeUnit> states, FederativeUnit state) {
        return query(states, state, 0, 20, null, null, " equipamento ");
    }

    private static SearchSolicitationsQuery query(
            Set<FederativeUnit> states,
            FederativeUnit state,
            int page,
            int size,
            LocalDate submittedFrom,
            LocalDate submittedTo,
            String text
    ) {
        return new SearchSolicitationsQuery(
                UUID.randomUUID(), states, text, null, null, null, state,
                submittedFrom, submittedTo, page, size, SearchSortField.SUBMITTED_AT,
                SearchDirection.DESC
        );
    }
}
