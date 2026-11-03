package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewResult;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StartSolicitationReviewResponse(
        UUID id,
        SolicitationStatus status,
        UUID assignedAnalystId,
        Instant reviewStartedAt
) {

    public static StartSolicitationReviewResponse from(StartSolicitationReviewResult result) {
        Objects.requireNonNull(result, "Start solicitation review result is required");
        return new StartSolicitationReviewResponse(
                result.id(), result.status(), result.assignedAnalystId(), result.reviewStartedAt()
        );
    }
}
