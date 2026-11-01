package br.com.sea.solicitations.user.application.port.in;

@FunctionalInterface
public interface AuthenticateUserUseCase {

    AuthenticationResult authenticate(AuthenticationCommand command);
}
