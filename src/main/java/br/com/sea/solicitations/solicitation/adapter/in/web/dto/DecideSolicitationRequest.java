package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.solicitation.application.port.in.DecisionOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecideSolicitationRequest(
        @NotNull(message = "Decision outcome is required")
        DecisionOutcome outcome,

        @NotBlank(message = "Decision comment is required")
        @Size(min = 10, max = 1000, message = "Decision comment must have between 10 and 1000 characters")
        String comment
) {
    public DecideSolicitationRequest {
        if (comment != null) {
            comment = comment.trim();
        }
    }
}
