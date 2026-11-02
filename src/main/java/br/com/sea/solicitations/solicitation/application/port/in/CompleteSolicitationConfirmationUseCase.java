package br.com.sea.solicitations.solicitation.application.port.in;

@FunctionalInterface
public interface CompleteSolicitationConfirmationUseCase {

    CompleteSolicitationConfirmationResult complete(CompleteSolicitationConfirmationCommand command);
}
