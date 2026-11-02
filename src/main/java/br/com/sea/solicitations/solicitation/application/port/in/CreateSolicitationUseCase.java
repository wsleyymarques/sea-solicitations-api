package br.com.sea.solicitations.solicitation.application.port.in;

@FunctionalInterface
public interface CreateSolicitationUseCase {

    CreateSolicitationResult create(CreateSolicitationCommand command);
}
