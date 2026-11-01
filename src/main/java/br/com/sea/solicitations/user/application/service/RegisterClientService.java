package br.com.sea.solicitations.user.application.service;

import br.com.sea.solicitations.user.application.exception.EmailAlreadyRegisteredException;
import br.com.sea.solicitations.user.application.port.in.RegisterClientCommand;
import br.com.sea.solicitations.user.application.port.in.RegisterClientResult;
import br.com.sea.solicitations.user.application.port.in.RegisterClientUseCase;
import br.com.sea.solicitations.user.application.port.out.PasswordHasher;
import br.com.sea.solicitations.user.application.port.out.UserRepository;
import br.com.sea.solicitations.user.domain.User;
import br.com.sea.solicitations.user.domain.UserRole;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class RegisterClientService implements RegisterClientUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterClientService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "Password hasher is required");
    }

    @Override
    @Transactional
    public RegisterClientResult register(RegisterClientCommand command) {
        Objects.requireNonNull(command, "Register client command is required");

        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyRegisteredException();
        }

        String passwordHash = passwordHasher.hash(command.rawPassword());
        User client = new User(command.name(), command.email(), passwordHash, UserRole.CLIENT);
        User savedClient = userRepository.save(client);

        return new RegisterClientResult(
                savedClient.getId(),
                savedClient.getName(),
                savedClient.getEmail(),
                savedClient.getRole()
        );
    }
}
