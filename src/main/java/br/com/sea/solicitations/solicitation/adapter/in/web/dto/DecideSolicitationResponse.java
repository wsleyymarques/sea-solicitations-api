package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationResult;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DecideSolicitationResponse(
        UUID id,
        SolicitationStatus status,
        UUID analystId,
        String comment,
        Instant decidedAt
) {
    public static DecideSolicitationResponse from(DecideSolicitationResult result) {
        Objects.requireNonNull(result, "Decide solicitation result is required");
        return new DecideSolicitationResponse(
                result.id(), result.status(), result.analystId(), result.comment(), result.decidedAt()
        );
    }
}
