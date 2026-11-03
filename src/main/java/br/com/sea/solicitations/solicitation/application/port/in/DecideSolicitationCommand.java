package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.shared.domain.FederativeUnit;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record DecideSolicitationCommand(
        UUID solicitationId,
        UUID analystId,
        Set<FederativeUnit> coveredStates,
        DecisionOutcome outcome,
        String comment
) {
    public DecideSolicitationCommand {
        Objects.requireNonNull(solicitationId, "Solicitation ID is required");
        Objects.requireNonNull(analystId, "Analyst ID is required");
        coveredStates = Set.copyOf(Objects.requireNonNull(coveredStates, "Covered states are required"));
        Objects.requireNonNull(outcome, "Decision outcome is required");
    }
}
