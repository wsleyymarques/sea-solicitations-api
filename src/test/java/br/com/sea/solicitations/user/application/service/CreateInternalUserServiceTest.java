package br.com.sea.solicitations.user.application.service;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.user.application.exception.EmailAlreadyRegisteredException;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserCommand;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserResult;
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

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateInternalUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private CreateInternalUserService service;

    @BeforeEach
    void setUp() {
        service = new CreateInternalUserService(userRepository, passwordHasher);
    }

    @Test
    void createsAnalystWithHashedPasswordAndStateCoverage() {
        CreateInternalUserCommand command = new CreateInternalUserCommand(
                "  Ana Silva  ",
                "ANA@EXAMPLE.COM",
                "raw-secret",
                UserRole.ANALYST,
                Set.of(FederativeUnit.SP, FederativeUnit.RJ)
        );
        UUID analystId = UUID.randomUUID();
        User savedAnalyst = org.mockito.Mockito.mock(User.class);

        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(passwordHasher.hash(command.rawPassword())).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenReturn(savedAnalyst);
        when(savedAnalyst.getId()).thenReturn(analystId);
        when(savedAnalyst.getName()).thenReturn("Ana Silva");
        when(savedAnalyst.getEmail()).thenReturn("ana@example.com");
        when(savedAnalyst.getRole()).thenReturn(UserRole.ANALYST);
        when(savedAnalyst.getAssignedStates()).thenReturn(Set.of(FederativeUnit.SP, FederativeUnit.RJ));

        CreateInternalUserResult result = service.create(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User analystToSave = userCaptor.getValue();

        assertThat(analystToSave.getName()).isEqualTo("Ana Silva");
        assertThat(analystToSave.getEmail()).isEqualTo("ana@example.com");
        assertThat(analystToSave.getPasswordHash()).isEqualTo("hashed-secret");
        assertThat(analystToSave.getRole()).isEqualTo(UserRole.ANALYST);
        assertThat(analystToSave.getAssignedStates()).containsExactlyInAnyOrder(
                FederativeUnit.SP,
                FederativeUnit.RJ
        );

        assertThat(result.id()).isEqualTo(analystId);
        assertThat(result.name()).isEqualTo("Ana Silva");
        assertThat(result.email()).isEqualTo("ana@example.com");
        assertThat(result.role()).isEqualTo(UserRole.ANALYST);
        assertThat(result.coveredStates()).containsExactlyInAnyOrder(FederativeUnit.SP, FederativeUnit.RJ);
    }

    @Test
    void createsAdministratorWithoutStateCoverage() {
        CreateInternalUserCommand command = new CreateInternalUserCommand(
                "Admin Two",
                "admin.two@example.com",
                "raw-secret",
                UserRole.ADMIN,
                Set.of()
        );
        User savedAdministrator = org.mockito.Mockito.mock(User.class);

        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(passwordHasher.hash(command.rawPassword())).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenReturn(savedAdministrator);
        when(savedAdministrator.getId()).thenReturn(UUID.randomUUID());
        when(savedAdministrator.getName()).thenReturn("Admin Two");
        when(savedAdministrator.getEmail()).thenReturn("admin.two@example.com");
        when(savedAdministrator.getRole()).thenReturn(UserRole.ADMIN);
        when(savedAdministrator.getAssignedStates()).thenReturn(Set.of());

        CreateInternalUserResult result = service.create(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(userCaptor.getValue().getAssignedStates()).isEmpty();
        assertThat(result.coveredStates()).isEmpty();
    }

    @Test
    void rejectsAlreadyRegisteredEmailBeforeHashingOrSaving() {
        CreateInternalUserCommand command = new CreateInternalUserCommand(
                "Ana Silva",
                "ana@example.com",
                "raw-secret",
                UserRole.ANALYST,
                Set.of(FederativeUnit.SP)
        );
        when(userRepository.existsByEmail(command.email())).thenReturn(true);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email is already registered");

        verifyNoInteractions(passwordHasher);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsClientRole() {
        assertThatThrownBy(() -> new CreateInternalUserCommand(
                "Client",
                "client@example.com",
                "raw-secret",
                UserRole.CLIENT,
                Set.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only analysts and administrators can be created");
    }

    @Test
    void rejectsAnalystWithoutCoverage() {
        assertThatThrownBy(() -> new CreateInternalUserCommand(
                "Analyst",
                "analyst@example.com",
                "raw-secret",
                UserRole.ANALYST,
                Set.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Analyst coverage must contain at least one state");
    }

    @Test
    void rejectsAdministratorWithCoverage() {
        assertThatThrownBy(() -> new CreateInternalUserCommand(
                "Admin",
                "admin@example.com",
                "raw-secret",
                UserRole.ADMIN,
                Set.of(FederativeUnit.SP)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Administrators cannot have state coverage");
    }

    @Test
    void commandDoesNotExposeRawPassword() {
        CreateInternalUserCommand command = new CreateInternalUserCommand(
                "Admin",
                "admin@example.com",
                "very-secret",
                UserRole.ADMIN,
                Set.of()
        );

        assertThat(command.toString())
                .doesNotContain("very-secret")
                .contains("[REDACTED]");
    }
}
