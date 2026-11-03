package br.com.sea.solicitations.user.adapter.out.persistence;

import br.com.sea.solicitations.user.application.exception.EmailAlreadyRegisteredException;
import br.com.sea.solicitations.user.application.port.out.UserRepository;
import br.com.sea.solicitations.user.domain.User;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserJpaRepository userJpaRepository;

    public JpaUserRepositoryAdapter(SpringDataUserJpaRepository userJpaRepository) {
        this.userJpaRepository = Objects.requireNonNull(userJpaRepository, "User JPA repository is required");
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmailIgnoreCase(normalizeEmail(email));
    }

    @Override
    public User save(User user) {
        try {
            return userJpaRepository.saveAndFlush(Objects.requireNonNull(user, "User is required"));
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, "uk_users_email")) {
                throw new EmailAlreadyRegisteredException();
            }
            throw exception;
        }
    }

    private static boolean hasConstraint(Throwable failure, String expectedConstraint) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && expectedConstraint.equalsIgnoreCase(constraintViolation.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim();
    }
}
