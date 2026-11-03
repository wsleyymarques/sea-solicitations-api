package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SearchSolicitationSummary(
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
}
