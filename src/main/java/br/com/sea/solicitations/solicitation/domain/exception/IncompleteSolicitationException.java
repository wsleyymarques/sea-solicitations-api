package br.com.sea.solicitations.solicitation.domain.exception;

import br.com.sea.solicitations.solicitation.domain.SolicitationStep;

import java.util.List;
import java.util.Objects;

public class IncompleteSolicitationException extends IllegalStateException {

    private final List<SolicitationStep> missingSteps;

    public IncompleteSolicitationException(List<SolicitationStep> missingSteps) {
        super("Solicitation is incomplete");
        Objects.requireNonNull(missingSteps, "Missing steps are required");
        if (missingSteps.isEmpty()) {
            throw new IllegalArgumentException("At least one missing step is required");
        }
        this.missingSteps = List.copyOf(missingSteps);
    }

    public List<SolicitationStep> missingSteps() {
        return missingSteps;
    }
}
