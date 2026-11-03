package br.com.sea.solicitations.solicitation.adapter.out.search;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.SearchUnavailableException;
import br.com.sea.solicitations.solicitation.application.port.in.SearchDirection;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationSummary;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsQuery;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsResult;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationSearchIndex;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.DateRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class ElasticsearchSolicitationSearchAdapter implements SolicitationSearchIndex {

    private final ElasticsearchOperations operations;
    private volatile boolean indexReady;

    public ElasticsearchSolicitationSearchAdapter(ElasticsearchOperations operations) {
        this.operations = Objects.requireNonNull(operations, "Elasticsearch operations are required");
    }

    @Override
    public void index(UUID id, Solicitation solicitation) {
        try {
            ensureIndex();
            operations.save(SolicitationSearchDocument.from(id, solicitation));
        } catch (RuntimeException exception) {
            throw new SearchUnavailableException(exception);
        }
    }

    @Override
    public SearchSolicitationsResult search(SearchSolicitationsQuery request) {
        Objects.requireNonNull(request, "Search query is required");
        if (request.coveredStates().isEmpty()) {
            return new SearchSolicitationsResult(List.of(), request.page(), request.size(), 0, 0);
        }
        try {
            ensureIndex();
            SearchHits<SolicitationSearchDocument> hits = operations.search(
                    buildQuery(request), SolicitationSearchDocument.class
            );
            long total = hits.getTotalHits();
            long pageCount = total / request.size() + (total % request.size() == 0 ? 0 : 1);
            int pages = (int) Math.min(Integer.MAX_VALUE, pageCount);
            return new SearchSolicitationsResult(
                    hits.getSearchHits().stream().map(hit -> summary(hit.getContent())).toList(),
                    request.page(), request.size(), total, pages
            );
        } catch (RuntimeException exception) {
            throw new SearchUnavailableException(exception);
        }
    }

    static NativeQuery buildQuery(SearchSolicitationsQuery request) {
        BoolQuery.Builder filters = new BoolQuery.Builder();
        filters.filter(terms("state", request.coveredStates().stream()
                .map(Enum::name).toList()));

        Query submitted = term("status", SolicitationStatus.SUBMITTED.name());
        Query assigned = Query.of(q -> q.bool(b -> b
                .filter(terms("status", List.of(
                        SolicitationStatus.IN_REVIEW.name(),
                        SolicitationStatus.APPROVED.name(),
                        SolicitationStatus.REJECTED.name()
                )))
                .filter(term("assignedAnalystId", request.analystId().toString()))));
        filters.filter(Query.of(q -> q.bool(b -> b
                .should(submitted, assigned).minimumShouldMatch("1"))));

        if (request.text() != null) {
            filters.must(Query.of(q -> q.multiMatch(m -> m
                    .fields("title", "description").query(request.text()))));
        }
        if (request.status() != null) {
            filters.filter(term("status", request.status().name()));
        }
        if (request.serviceType() != null) {
            filters.filter(term("serviceType", request.serviceType().name()));
        }
        if (request.priority() != null) {
            filters.filter(term("priority", request.priority().name()));
        }
        if (request.state() != null) {
            filters.filter(term("state", request.state().name()));
        }
        boolean hasUpperDateBound = request.submittedTo() != null
                && !request.submittedTo().equals(LocalDate.MAX);
        if (request.submittedFrom() != null || hasUpperDateBound) {
            DateRangeQuery.Builder date = new DateRangeQuery.Builder().field("submittedAt");
            if (request.submittedFrom() != null) {
                date.gte(startOfDay(request.submittedFrom()));
            }
            if (hasUpperDateBound) {
                date.lt(startOfDay(request.submittedTo().plusDays(1)));
            }
            filters.filter(Query.of(q -> q.range(r -> r.date(date.build()))));
        }

        Sort.Direction direction = request.direction() == SearchDirection.ASC
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(
                new Sort.Order(direction, request.sortBy().fieldName()),
                new Sort.Order(direction, "sortId")
        );
        NativeQueryBuilder query = NativeQuery.builder()
                .withQuery(filters.build()._toQuery())
                .withPageable(PageRequest.of(request.page(), request.size()))
                .withSort(sort)
                .withTrackTotalHits(true);
        return query.build();
    }

    private static Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value)));
    }

    private static Query terms(String field, List<String> values) {
        return Query.of(q -> q.terms(t -> t.field(field).terms(v -> v.value(
                values.stream().map(FieldValue::of).toList()
        ))));
    }

    private static String startOfDay(LocalDate day) {
        return day.atStartOfDay().toInstant(ZoneOffset.UTC).toString();
    }

    private static SearchSolicitationSummary summary(SolicitationSearchDocument document) {
        return new SearchSolicitationSummary(
                UUID.fromString(document.id()),
                SolicitationStatus.valueOf(document.status()),
                ServiceType.valueOf(document.serviceType()),
                document.title(),
                document.priority() == null ? null : Priority.valueOf(document.priority()),
                FederativeUnit.valueOf(document.state()),
                document.createdAt(),
                document.submittedAt(),
                document.preferredDate(),
                document.assignedAnalystId() == null
                        ? null : UUID.fromString(document.assignedAnalystId())
        );
    }

    private void ensureIndex() {
        if (indexReady) {
            return;
        }
        synchronized (this) {
            if (indexReady) {
                return;
            }
            IndexOperations index = operations.indexOps(SolicitationSearchDocument.class);
            if (!index.exists()) {
                index.createWithMapping();
            }
            indexReady = true;
        }
    }
}
