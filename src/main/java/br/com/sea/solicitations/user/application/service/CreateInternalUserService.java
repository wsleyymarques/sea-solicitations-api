package br.com.sea.solicitations.user.application.service;

import br.com.sea.solicitations.audit.application.annotation.Audited;
import br.com.sea.solicitations.audit.domain.AuditAction;
import br.com.sea.solicitations.user.application.exception.EmailAlreadyRegisteredException;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserCommand;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserResult;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserUseCase;
import br.com.sea.solicitations.user.application.port.out.PasswordHasher;
import br.com.sea.solicitations.user.application.port.out.UserRepository;
import br.com.sea.solicitations.user.domain.User;
import br.com.sea.solicitations.user.domain.UserRole;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class CreateInternalUserService implements CreateInternalUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public CreateInternalUserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "Password hasher is required");
    }

    @Override
    @Audited(AuditAction.USER_CREATED)
    @Transactional
    public CreateInternalUserResult create(CreateInternalUserCommand command) {
        Objects.requireNonNull(command, "Create internal user command is required");

        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = new User(
                command.name(),
                command.email(),
                passwordHasher.hash(command.rawPassword()),
                command.role()
        );
        if (command.role() == UserRole.ANALYST) {
            user.assignStates(command.coveredStates());
        }

        User savedUser = userRepository.save(user);
        return new CreateInternalUserResult(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getAssignedStates()
        );
    }
}
