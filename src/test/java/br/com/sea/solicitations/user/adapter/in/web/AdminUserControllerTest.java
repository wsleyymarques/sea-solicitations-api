package br.com.sea.solicitations.user.adapter.in.web;

import br.com.sea.solicitations.shared.adapter.in.web.error.GlobalExceptionHandler;
import br.com.sea.solicitations.shared.adapter.in.web.security.SecurityConfiguration;
import br.com.sea.solicitations.shared.adapter.in.web.security.SecurityErrorHandler;
import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.user.application.exception.EmailAlreadyRegisteredException;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserCommand;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserResult;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserUseCase;
import br.com.sea.solicitations.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfiguration.class, SecurityErrorHandler.class, GlobalExceptionHandler.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateInternalUserUseCase useCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void administratorCreatesAnalyst() throws Exception {
        UUID analystId = UUID.randomUUID();
        when(useCase.create(any(CreateInternalUserCommand.class))).thenReturn(
                new CreateInternalUserResult(
                        analystId,
                        "Ana Silva",
                        "ana@example.com",
                        UserRole.ANALYST,
                        Set.of(FederativeUnit.SP, FederativeUnit.RJ)
                )
        );

        mockMvc.perform(post("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ana Silva",
                                  "email": "ana@example.com",
                                  "password": "StrongPassword@123",
                                  "role": "ANALYST",
                                  "coveredStates": ["SP", "RJ"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(analystId.toString()))
                .andExpect(jsonPath("$.name").value("Ana Silva"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.role").value("ANALYST"))
                .andExpect(jsonPath("$.coveredStates").isArray())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        ArgumentCaptor<CreateInternalUserCommand> captor =
                ArgumentCaptor.forClass(CreateInternalUserCommand.class);
        verify(useCase).create(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Ana Silva");
        assertThat(captor.getValue().email()).isEqualTo("ana@example.com");
        assertThat(captor.getValue().rawPassword()).isEqualTo("StrongPassword@123");
        assertThat(captor.getValue().role()).isEqualTo(UserRole.ANALYST);
        assertThat(captor.getValue().coveredStates()).containsExactlyInAnyOrder(
                FederativeUnit.SP,
                FederativeUnit.RJ
        );
    }

    @Test
    void administratorCreatesAnotherAdministratorWithOmittedCoverage() throws Exception {
        UUID administratorId = UUID.randomUUID();
        when(useCase.create(any(CreateInternalUserCommand.class))).thenReturn(
                new CreateInternalUserResult(
                        administratorId,
                        "Admin Two",
                        "admin.two@example.com",
                        UserRole.ADMIN,
                        Set.of()
                )
        );

        mockMvc.perform(post("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Admin Two",
                                  "email": "admin.two@example.com",
                                  "password": "StrongPassword@123",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(administratorId.toString()))
                .andExpect(jsonPath("$.coveredStates").isEmpty());

        ArgumentCaptor<CreateInternalUserCommand> captor =
                ArgumentCaptor.forClass(CreateInternalUserCommand.class);
        verify(useCase).create(captor.capture());
        assertThat(captor.getValue().coveredStates()).isEmpty();
    }

    @Test
    void clientCannotCreateInternalUsers() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(APPLICATION_JSON)
                        .content(validAnalystJson()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }

    @Test
    void analystCannotCreateInternalUsers() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ANALYST")))
                        .contentType(APPLICATION_JSON)
                        .content(validAnalystJson()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }

    @Test
    void rejectsInvalidFieldsBeforeCallingUseCase() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "email": "not-an-email",
                                  "password": "short",
                                  "role": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(useCase);
    }

    @Test
    void rejectsClientRoleForInternalUser() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Client",
                                  "email": "client@example.com",
                                  "password": "StrongPassword@123",
                                  "role": "CLIENT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(useCase);
    }

    @Test
    void rejectsAnalystWithoutCoverageAndAdministratorWithCoverage() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Analyst",
                                  "email": "analyst@example.com",
                                  "password": "StrongPassword@123",
                                  "role": "ANALYST"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Admin",
                                  "email": "admin@example.com",
                                  "password": "StrongPassword@123",
                                  "role": "ADMIN",
                                  "coveredStates": ["SP"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(useCase);
    }

    @Test
    void rejectsNullElementInsideCoverageAsValidationError() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Analyst",
                                  "email": "analyst@example.com",
                                  "password": "StrongPassword@123",
                                  "role": "ANALYST",
                                  "coveredStates": ["SP", null]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.violations[0].field").value("coveredStates[]"));

        verifyNoInteractions(useCase);
    }

    @Test
    void mapsDuplicateEmailToConflict() throws Exception {
        when(useCase.create(any(CreateInternalUserCommand.class)))
                .thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content(validAnalystJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    private static String validAnalystJson() {
        return """
                {
                  "name": "Ana Silva",
                  "email": "ana@example.com",
                  "password": "StrongPassword@123",
                  "role": "ANALYST",
                  "coveredStates": ["SP"]
                }
                """;
    }
}
