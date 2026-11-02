package br.com.sea.solicitations.solicitation.application.port.in;

@FunctionalInterface
public interface SubmitSolicitationUseCase {

    SubmitSolicitationResult submit(SubmitSolicitationCommand command);
}
