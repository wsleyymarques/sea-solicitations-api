package br.com.sea.solicitations.user.application.service;

import br.com.sea.solicitations.user.application.exception.InvalidCredentialsException;
import br.com.sea.solicitations.user.application.port.in.AuthenticateUserUseCase;
import br.com.sea.solicitations.user.application.port.in.AuthenticationCommand;
import br.com.sea.solicitations.user.application.port.in.AuthenticationResult;
import br.com.sea.solicitations.user.application.port.out.AccessTokenClaims;
import br.com.sea.solicitations.user.application.port.out.AccessTokenGenerator;
import br.com.sea.solicitations.user.application.port.out.GeneratedAccessToken;
import br.com.sea.solicitations.user.application.port.out.PasswordHasher;
import br.com.sea.solicitations.user.application.port.out.UserRepository;
import br.com.sea.solicitations.user.domain.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class AuthenticateUserService implements AuthenticateUserUseCase {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AccessTokenGenerator accessTokenGenerator;

    public AuthenticateUserService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            AccessTokenGenerator accessTokenGenerator
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "Password hasher is required");
        this.accessTokenGenerator = Objects.requireNonNull(accessTokenGenerator, "Access token generator is required");
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResult authenticate(AuthenticationCommand command) {
        Objects.requireNonNull(command, "Authentication command is required");

        User user = userRepository.findByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        boolean passwordMatches = passwordHasher.matches(
                command.rawPassword(),
                user.getPasswordHash()
        );

        if (!passwordMatches || !user.isActive()) {
            throw new InvalidCredentialsException();
        }

        AccessTokenClaims claims = new AccessTokenClaims(
                user.getId(),
                user.getRole(),
                user.getAssignedStates()
        );
        GeneratedAccessToken generatedToken = Objects.requireNonNull(
                accessTokenGenerator.generate(claims),
                "Generated access token is required"
        );

        return new AuthenticationResult(
                generatedToken.value(),
                TOKEN_TYPE,
                generatedToken.expiresIn()
        );
    }
}
