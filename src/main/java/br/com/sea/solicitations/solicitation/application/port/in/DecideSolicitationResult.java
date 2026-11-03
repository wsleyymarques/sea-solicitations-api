package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.time.Instant;
import java.util.UUID;

public record DecideSolicitationResult(
        UUID id,
        SolicitationStatus status,
        UUID analystId,
        String comment,
        Instant decidedAt
) {
}
