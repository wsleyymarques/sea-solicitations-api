package br.com.sea.solicitations.solicitation.adapter.out.search;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.SearchUnavailableException;
import br.com.sea.solicitations.solicitation.application.port.in.SearchDirection;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsQuery;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSortField;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticsearchSolicitationSearchAdapterTest {

    @Mock
    private ElasticsearchOperations operations;

    @Mock
    private IndexOperations indexOperations;

    private ElasticsearchSolicitationSearchAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ElasticsearchSolicitationSearchAdapter(operations);
    }

    @Test
    void createsExplicitMappingAndIndexesBySolicitationId() {
        when(operations.indexOps(SolicitationSearchDocument.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(false);
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        UUID id = UUID.randomUUID();

        adapter.index(id, solicitation);

        verify(indexOperations).createWithMapping();
        ArgumentCaptor<SolicitationSearchDocument> captor =
                ArgumentCaptor.forClass(SolicitationSearchDocument.class);
        verify(operations).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(id.toString());
        assertThat(captor.getValue().status()).isEqualTo("DRAFT");
    }

    @Test
    void surfacesUnavailableSearchEngineAsServiceError() {
        when(operations.indexOps(SolicitationSearchDocument.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);
        when(operations.save(any(SolicitationSearchDocument.class)))
                .thenThrow(new DataAccessResourceFailureException("unavailable"));

        assertThatThrownBy(() -> adapter.index(UUID.randomUUID(), new Solicitation(UUID.randomUUID())))
                .isInstanceOf(SearchUnavailableException.class);
    }

    @Test
    void buildsMandatoryCoverageAndAssignmentAlongsideAllFilters() {
        UUID analystId = UUID.randomUUID();
        SearchSolicitationsQuery request = new SearchSolicitationsQuery(
                analystId, Set.of(FederativeUnit.SP, FederativeUnit.RJ), "equipamento",
                SolicitationStatus.APPROVED, ServiceType.INSTALLATION, Priority.HIGH,
                FederativeUnit.SP, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3),
                2, 25, SearchSortField.CREATED_AT, SearchDirection.ASC
        );

        NativeQuery query = ElasticsearchSolicitationSearchAdapter.buildQuery(request);
        String json = query.getQuery().toString();

        assertThat(json).contains("state").doesNotContain("DRAFT");
        assertThat(json).contains("SP", "RJ", "SUBMITTED", "IN_REVIEW", "APPROVED",
                "REJECTED", analystId.toString(), "equipamento", "INSTALLATION", "HIGH");
        assertThat(json).contains("2026-11-01T00:00:00Z", "2026-11-04T00:00:00Z");
        assertThat(query.getQuery().bool().filter().get(0).terms().field()).isEqualTo("state");
        assertThat(query.getQuery().bool().filter().get(1).bool().minimumShouldMatch()).isEqualTo("1");
        assertThat(query.getQuery().bool().filter().get(1).bool().should().get(1)
                .bool().filter().get(1).term().field()).isEqualTo("assignedAnalystId");
        assertThat(query.getPageable().getPageNumber()).isEqualTo(2);
        assertThat(query.getPageable().getPageSize()).isEqualTo(25);
        assertThat(query.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(query.getSort().getOrderFor("sortId").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(query.getTrackTotalHits()).isTrue();
    }

    @Test
    void mapsSearchHitsAndTotalCountToPage() {
        when(operations.indexOps(SolicitationSearchDocument.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);
        @SuppressWarnings("unchecked")
        SearchHits<SolicitationSearchDocument> hits = org.mockito.Mockito.mock(SearchHits.class);
        @SuppressWarnings("unchecked")
        SearchHit<SolicitationSearchDocument> hit = org.mockito.Mockito.mock(SearchHit.class);
        UUID id = UUID.randomUUID();
        when(hit.getContent()).thenReturn(new SolicitationSearchDocument(
                id.toString(), id.toString(), "SUBMITTED", "INSTALLATION",
                "Instalacao de equipamento", "Descricao suficiente para busca", "HIGH", "SP",
                LocalDate.of(2026, 11, 4), Instant.parse("2026-11-01T10:00:00Z"),
                Instant.parse("2026-11-02T12:00:00Z"), null, null
        ));
        when(hits.getTotalHits()).thenReturn(21L);
        when(hits.getSearchHits()).thenReturn(List.of(hit));
        when(operations.search(any(NativeQuery.class), eq(SolicitationSearchDocument.class)))
                .thenReturn(hits);
        SearchSolicitationsQuery request = new SearchSolicitationsQuery(
                UUID.randomUUID(), Set.of(FederativeUnit.SP), null, null, null, null,
                null, null, null, 1, 20, SearchSortField.SUBMITTED_AT, SearchDirection.DESC
        );

        var page = adapter.search(request);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().id()).isEqualTo(id);
        assertThat(page.content().getFirst().status()).isEqualTo(SolicitationStatus.SUBMITTED);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.totalElements()).isEqualTo(21);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void handlesMaximumSubmittedToWithoutDateOverflow() {
        SearchSolicitationsQuery request = new SearchSolicitationsQuery(
                UUID.randomUUID(), Set.of(FederativeUnit.SP), null, null, null, null,
                null, null, LocalDate.MAX, 0, 20,
                SearchSortField.SUBMITTED_AT, SearchDirection.DESC
        );

        assertThat(ElasticsearchSolicitationSearchAdapter.buildQuery(request)
                .getQuery().toString()).doesNotContain("999999999");
    }
}
