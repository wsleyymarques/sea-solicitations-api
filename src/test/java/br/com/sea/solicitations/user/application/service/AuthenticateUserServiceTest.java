package br.com.sea.solicitations.user.application.service;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.user.application.exception.InvalidCredentialsException;
import br.com.sea.solicitations.user.application.port.in.AuthenticationCommand;
import br.com.sea.solicitations.user.application.port.in.AuthenticationResult;
import br.com.sea.solicitations.user.application.port.out.AccessTokenClaims;
import br.com.sea.solicitations.user.application.port.out.AccessTokenGenerator;
import br.com.sea.solicitations.user.application.port.out.GeneratedAccessToken;
import br.com.sea.solicitations.user.application.port.out.PasswordHasher;
import br.com.sea.solicitations.user.application.port.out.UserRepository;
import br.com.sea.solicitations.user.domain.User;
import br.com.sea.solicitations.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private AccessTokenGenerator accessTokenGenerator;

    @Mock
    private User user;

    private AuthenticateUserService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticateUserService(
                userRepository,
                passwordHasher,
                accessTokenGenerator
        );
    }

    @Test
    void authenticatesActiveUserAndGeneratesAccessToken() {
        AuthenticationCommand command = command();
        UUID userId = UUID.randomUUID();
        Set<FederativeUnit> coveredStates = Set.of(FederativeUnit.SP, FederativeUnit.RJ);

        when(userRepository.findByEmail(command.email())).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("stored-password-hash");
        when(passwordHasher.matches(command.rawPassword(), "stored-password-hash")).thenReturn(true);
        when(user.isActive()).thenReturn(true);
        when(user.getId()).thenReturn(userId);
        when(user.getRole()).thenReturn(UserRole.ANALYST);
        when(user.getAssignedStates()).thenReturn(coveredStates);
        when(accessTokenGenerator.generate(org.mockito.ArgumentMatchers.any(AccessTokenClaims.class)))
                .thenReturn(new GeneratedAccessToken("signed.jwt.value", 3600L));

        AuthenticationResult result = service.authenticate(command);

        ArgumentCaptor<AccessTokenClaims> claimsCaptor = ArgumentCaptor.forClass(AccessTokenClaims.class);
        verify(accessTokenGenerator).generate(claimsCaptor.capture());

        assertThat(claimsCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(claimsCaptor.getValue().role()).isEqualTo(UserRole.ANALYST);
        assertThat(claimsCaptor.getValue().coveredStates()).containsExactlyInAnyOrderElementsOf(coveredStates);
        assertThat(result.accessToken()).isEqualTo("signed.jwt.value");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void rejectsUnknownEmailWithoutCheckingPasswordOrGeneratingToken() {
        AuthenticationCommand command = command();
        when(userRepository.findByEmail(command.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(command))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verifyNoInteractions(passwordHasher, accessTokenGenerator);
    }

    @Test
    void rejectsIncorrectPasswordWithoutGeneratingToken() {
        AuthenticationCommand command = command();
        when(userRepository.findByEmail(command.email())).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("stored-password-hash");
        when(passwordHasher.matches(command.rawPassword(), "stored-password-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate(command))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verifyNoInteractions(accessTokenGenerator);
    }

    @Test
    void rejectsInactiveUserWithoutGeneratingToken() {
        AuthenticationCommand command = command();
        when(userRepository.findByEmail(command.email())).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("stored-password-hash");
        when(passwordHasher.matches(command.rawPassword(), "stored-password-hash")).thenReturn(true);
        when(user.isActive()).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate(command))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verifyNoInteractions(accessTokenGenerator);
    }

    private static AuthenticationCommand command() {
        return new AuthenticationCommand("user@example.com", "correct-password");
    }
}
