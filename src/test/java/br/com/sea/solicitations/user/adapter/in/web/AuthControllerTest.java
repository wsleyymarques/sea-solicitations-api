package br.com.sea.solicitations.user.adapter.in.web;

import br.com.sea.solicitations.shared.adapter.in.web.error.GlobalExceptionHandler;
import br.com.sea.solicitations.user.application.exception.EmailAlreadyRegisteredException;
import br.com.sea.solicitations.user.application.exception.InvalidCredentialsException;
import br.com.sea.solicitations.user.application.port.in.AuthenticateUserUseCase;
import br.com.sea.solicitations.user.application.port.in.AuthenticationCommand;
import br.com.sea.solicitations.user.application.port.in.AuthenticationResult;
import br.com.sea.solicitations.user.application.port.in.RegisterClientCommand;
import br.com.sea.solicitations.user.application.port.in.RegisterClientResult;
import br.com.sea.solicitations.user.application.port.in.RegisterClientUseCase;
import br.com.sea.solicitations.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private RegisterClientUseCase registerClientUseCase;
    private AuthenticateUserUseCase authenticateUserUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        registerClientUseCase = mock(RegisterClientUseCase.class);
        authenticateUserUseCase = mock(AuthenticateUserUseCase.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(registerClientUseCase, authenticateUserUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registersClientAndReturnsCreated() throws Exception {
        UUID clientId = UUID.randomUUID();
        when(registerClientUseCase.register(any(RegisterClientCommand.class)))
                .thenReturn(new RegisterClientResult(
                        clientId,
                        "Joao Souza",
                        "joao@example.com",
                        UserRole.CLIENT
                ));

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Joao Souza",
                                  "email": "JOAO@EXAMPLE.COM",
                                  "password": "StrongPassword@123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(clientId.toString()))
                .andExpect(jsonPath("$.name").value("Joao Souza"))
                .andExpect(jsonPath("$.email").value("joao@example.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.password").doesNotExist());

        ArgumentCaptor<RegisterClientCommand> commandCaptor =
                ArgumentCaptor.forClass(RegisterClientCommand.class);
        verify(registerClientUseCase).register(commandCaptor.capture());
        RegisterClientCommand command = commandCaptor.getValue();

        org.assertj.core.api.Assertions.assertThat(command.name()).isEqualTo("Joao Souza");
        org.assertj.core.api.Assertions.assertThat(command.email()).isEqualTo("JOAO@EXAMPLE.COM");
        org.assertj.core.api.Assertions.assertThat(command.rawPassword()).isEqualTo("StrongPassword@123");
    }

    @Test
    void rejectsInvalidRequestBeforeCallingUseCase() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "invalid-email",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.violations.length()").value(3))
                .andExpect(jsonPath("$.violations[0].field").value("email"))
                .andExpect(jsonPath("$.violations[1].field").value("name"))
                .andExpect(jsonPath("$.violations[2].field").value("password"));

        verifyNoInteractions(registerClientUseCase);
    }

    @Test
    void returnsConflictWhenEmailIsAlreadyRegistered() throws Exception {
        when(registerClientUseCase.register(any(RegisterClientCommand.class)))
                .thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Joao Souza",
                                  "email": "joao@example.com",
                                  "password": "StrongPassword@123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.message").value("Email is already registered"))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.violations").doesNotExist());
    }

    @Test
    void authenticatesUserAndReturnsBearerToken() throws Exception {
        when(authenticateUserUseCase.authenticate(any(AuthenticationCommand.class)))
                .thenReturn(new AuthenticationResult("signed.jwt.value", "Bearer", 3600L));

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "USER@EXAMPLE.COM",
                                  "password": "StrongPassword@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.password").doesNotExist());

        ArgumentCaptor<AuthenticationCommand> commandCaptor =
                ArgumentCaptor.forClass(AuthenticationCommand.class);
        verify(authenticateUserUseCase).authenticate(commandCaptor.capture());

        org.assertj.core.api.Assertions.assertThat(commandCaptor.getValue().email())
                .isEqualTo("user@example.com");
        org.assertj.core.api.Assertions.assertThat(commandCaptor.getValue().rawPassword())
                .isEqualTo("StrongPassword@123");
    }

    @Test
    void returnsUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        when(authenticateUserUseCase.authenticate(any(AuthenticationCommand.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "WrongPassword"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    void rejectsInvalidLoginRequestBeforeCallingUseCase() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/auth/login"))
                .andExpect(jsonPath("$.violations.length()").value(2));

        verifyNoInteractions(authenticateUserUseCase);
    }
}
