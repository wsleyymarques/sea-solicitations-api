package br.com.sea.solicitations.user.application.port.in;

public interface CreateInternalUserUseCase {

    CreateInternalUserResult create(CreateInternalUserCommand command);
}
