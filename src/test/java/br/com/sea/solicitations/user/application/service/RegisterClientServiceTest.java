package br.com.sea.solicitations.user.application.service;

import br.com.sea.solicitations.user.application.exception.EmailAlreadyRegisteredException;
import br.com.sea.solicitations.user.application.port.in.RegisterClientCommand;
import br.com.sea.solicitations.user.application.port.in.RegisterClientResult;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterClientServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private RegisterClientService service;

    @BeforeEach
    void setUp() {
        service = new RegisterClientService(userRepository, passwordHasher);
    }

    @Test
    void registersClientWithHashedPassword() {
        RegisterClientCommand command = new RegisterClientCommand(
                "Joao Souza",
                "JOAO@EXAMPLE.COM",
                "raw-secret"
        );
        UUID clientId = UUID.randomUUID();
        User savedClient = org.mockito.Mockito.mock(User.class);

        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(passwordHasher.hash(command.rawPassword())).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenReturn(savedClient);
        when(savedClient.getId()).thenReturn(clientId);
        when(savedClient.getName()).thenReturn("Joao Souza");
        when(savedClient.getEmail()).thenReturn("joao@example.com");
        when(savedClient.getRole()).thenReturn(UserRole.CLIENT);

        RegisterClientResult result = service.register(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User clientToSave = userCaptor.getValue();

        assertThat(clientToSave.getName()).isEqualTo("Joao Souza");
        assertThat(clientToSave.getEmail()).isEqualTo("joao@example.com");
        assertThat(clientToSave.getPasswordHash()).isEqualTo("hashed-secret");
        assertThat(clientToSave.getRole()).isEqualTo(UserRole.CLIENT);
        assertThat(clientToSave.isActive()).isTrue();

        assertThat(result.id()).isEqualTo(clientId);
        assertThat(result.name()).isEqualTo("Joao Souza");
        assertThat(result.email()).isEqualTo("joao@example.com");
        assertThat(result.role()).isEqualTo(UserRole.CLIENT);
    }

    @Test
    void rejectsAlreadyRegisteredEmailBeforeHashingOrSaving() {
        RegisterClientCommand command = new RegisterClientCommand(
                "Joao Souza",
                "joao@example.com",
                "raw-secret"
        );
        when(userRepository.existsByEmail(command.email())).thenReturn(true);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email is already registered");

        verifyNoInteractions(passwordHasher);
        verify(userRepository, never()).save(any(User.class));
    }
}
