package br.com.sea.solicitations.solicitation.application.port.in;

public interface DecideSolicitationUseCase {
    DecideSolicitationResult decide(DecideSolicitationCommand command);
}
