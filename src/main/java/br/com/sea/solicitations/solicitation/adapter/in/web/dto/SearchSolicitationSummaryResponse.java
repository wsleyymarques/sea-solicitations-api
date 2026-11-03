package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationSummary;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SearchSolicitationSummaryResponse(
        UUID id,
        SolicitationStatus status,
        ServiceType serviceType,
        String title,
        Priority priority,
        FederativeUnit state,
        Instant createdAt,
        Instant submittedAt,
        LocalDate preferredDate,
        UUID assignedAnalystId
) {
    public static SearchSolicitationSummaryResponse from(SearchSolicitationSummary summary) {
        return new SearchSolicitationSummaryResponse(
                summary.id(), summary.status(), summary.serviceType(), summary.title(),
                summary.priority(), summary.state(), summary.createdAt(), summary.submittedAt(),
                summary.preferredDate(), summary.assignedAnalystId()
        );
    }
}
