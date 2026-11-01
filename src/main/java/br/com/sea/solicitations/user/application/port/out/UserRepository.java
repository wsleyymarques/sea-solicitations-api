package br.com.sea.solicitations.user.application.port.out;

import br.com.sea.solicitations.user.domain.User;

import java.util.Optional;

/**
 * Output port used by the application to persist users.
 *
 * <p>E-mail lookup is case-insensitive and ignores surrounding whitespace.</p>
 */
public interface UserRepository {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    User save(User user);
}
