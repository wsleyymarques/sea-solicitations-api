package br.com.sea.solicitations.user.application.port.in;

/**
 * Input port for registering a new client.
 *
 * <p>The implementation must always create an active user with the CLIENT
 * role, normalize the e-mail, reject duplicates and hash the raw password
 * before persistence.</p>
 */
@FunctionalInterface
public interface RegisterClientUseCase {

    RegisterClientResult register(RegisterClientCommand command);
}
