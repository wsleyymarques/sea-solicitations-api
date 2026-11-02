package br.com.sea.solicitations.solicitation.adapter.in.web;

import br.com.sea.solicitations.shared.adapter.in.web.error.GlobalExceptionHandler;
import br.com.sea.solicitations.shared.adapter.in.web.security.SecurityConfiguration;
import br.com.sea.solicitations.shared.adapter.in.web.security.SecurityErrorHandler;
import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.InvalidSolicitationStateException;
import br.com.sea.solicitations.solicitation.application.exception.PostalCodeNotFoundException;
import br.com.sea.solicitations.solicitation.application.exception.PostalCodeServiceUnavailableException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationConcurrentUpdateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressResult;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationUseCase;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolicitationController.class)
@Import({
        SecurityConfiguration.class,
        SecurityErrorHandler.class,
        GlobalExceptionHandler.class
})
class SolicitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSolicitationUseCase createSolicitationUseCase;

    @MockitoBean
    private CompleteSolicitationAddressUseCase completeSolicitationAddressUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void createsSolicitationForAuthenticatedClient() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        when(createSolicitationUseCase.create(any(CreateSolicitationCommand.class)))
                .thenReturn(new CreateSolicitationResult(
                        solicitationId,
                        SolicitationStatus.DRAFT,
                        SolicitationStep.ADDRESS
                ));

        mockMvc.perform(post("/solicitations")
                        .with(jwt()
                                .jwt(token -> token.subject(clientId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceType": "INSTALLATION",
                                  "title": "Instalacao de equipamento",
                                  "description": "Solicito a instalacao de um novo equipamento no endereco informado."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/solicitations/" + solicitationId))
                .andExpect(jsonPath("$.id").value(solicitationId.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentStep").value("ADDRESS"));

        ArgumentCaptor<CreateSolicitationCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateSolicitationCommand.class);
        verify(createSolicitationUseCase).create(commandCaptor.capture());

        CreateSolicitationCommand command = commandCaptor.getValue();
        assertThat(command.clientId()).isEqualTo(clientId);
        assertThat(command.serviceType().name()).isEqualTo("INSTALLATION");
        assertThat(command.title()).isEqualTo("Instalacao de equipamento");
        assertThat(command.description())
                .isEqualTo("Solicito a instalacao de um novo equipamento no endereco informado.");
    }

    @Test
    void rejectsInvalidBasicDataBeforeCallingUseCase() throws Exception {
        mockMvc.perform(post("/solicitations")
                        .with(jwt()
                                .jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "ab",
                                  "description": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/solicitations"))
                .andExpect(jsonPath("$.violations.length()").value(3))
                .andExpect(jsonPath("$.violations[0].field").value("description"))
                .andExpect(jsonPath("$.violations[1].field").value("serviceType"))
                .andExpect(jsonPath("$.violations[2].field").value("title"));

        verifyNoInteractions(createSolicitationUseCase);
    }

    @Test
    void rejectsBasicDataThatIsTooShortAfterTrimming() throws Exception {
        mockMvc.perform(post("/solicitations")
                        .with(jwt()
                                .jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceType": "INSTALLATION",
                                  "title": " ab ",
                                  "description": "short               "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.violations.length()").value(2))
                .andExpect(jsonPath("$.violations[0].field").value("description"))
                .andExpect(jsonPath("$.violations[1].field").value("title"));

        verifyNoInteractions(createSolicitationUseCase);
    }

    @Test
    void rejectsUnknownServiceTypeAsMalformedJson() throws Exception {
        mockMvc.perform(post("/solicitations")
                        .with(jwt()
                                .jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceType": "UNKNOWN",
                                  "title": "Instalacao de equipamento",
                                  "description": "Solicito a instalacao de um novo equipamento no endereco informado."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.path").value("/solicitations"));

        verifyNoInteractions(createSolicitationUseCase);
    }

    @Test
    void completesAddressForAuthenticatedOwnerUsingOnlyClientInputFields() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        when(completeSolicitationAddressUseCase.complete(any(CompleteSolicitationAddressCommand.class)))
                .thenReturn(new CompleteSolicitationAddressResult(
                        solicitationId,
                        SolicitationStatus.DRAFT,
                        SolicitationStep.CONFIRMATION,
                        "01001000",
                        "Pra\u00e7a da S\u00e9",
                        "S\u00e9",
                        "S\u00e3o Paulo",
                        FederativeUnit.SP,
                        "100",
                        "Apto 12"
                ));

        mockMvc.perform(put("/solicitations/{id}/address", solicitationId)
                        .with(jwt()
                                .jwt(token -> token.subject(clientId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "postalCode": "01001-000",
                                  "number": "100",
                                  "complement": "Apto 12",
                                  "street": "Rua enviada pelo cliente",
                                  "state": "RJ"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(solicitationId.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentStep").value("CONFIRMATION"))
                .andExpect(jsonPath("$.address.postalCode").value("01001000"))
                .andExpect(jsonPath("$.address.street").value("Pra\u00e7a da S\u00e9"))
                .andExpect(jsonPath("$.address.neighborhood").value("S\u00e9"))
                .andExpect(jsonPath("$.address.city").value("S\u00e3o Paulo"))
                .andExpect(jsonPath("$.address.state").value("SP"))
                .andExpect(jsonPath("$.address.number").value("100"))
                .andExpect(jsonPath("$.address.complement").value("Apto 12"));

        ArgumentCaptor<CompleteSolicitationAddressCommand> commandCaptor =
                ArgumentCaptor.forClass(CompleteSolicitationAddressCommand.class);
        verify(completeSolicitationAddressUseCase).complete(commandCaptor.capture());

        CompleteSolicitationAddressCommand command = commandCaptor.getValue();
        assertThat(command.solicitationId()).isEqualTo(solicitationId);
        assertThat(command.clientId()).isEqualTo(clientId);
        assertThat(command.postalCode()).isEqualTo("01001000");
        assertThat(command.number()).isEqualTo("100");
        assertThat(command.complement()).isEqualTo("Apto 12");
    }

    @Test
    void rejectsInvalidAddressBeforeCallingUseCase() throws Exception {
        UUID solicitationId = UUID.randomUUID();

        mockMvc.perform(put("/solicitations/{id}/address", solicitationId)
                        .with(jwt()
                                .jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "postalCode": "invalid",
                                  "number": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.violations.length()").value(2));

        verifyNoInteractions(completeSolicitationAddressUseCase);
    }

    @Test
    void rejectsMalformedSolicitationIdAsValidationError() throws Exception {
        mockMvc.perform(put("/solicitations/not-a-uuid/address")
                        .with(jwt()
                                .jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "postalCode": "01001000",
                                  "number": "100"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.violations[0].field").value("id"))
                .andExpect(jsonPath("$.violations[0].message").value("must be a valid UUID"));

        verifyNoInteractions(completeSolicitationAddressUseCase);
    }

    @Test
    void returnsUnprocessableEntityWhenPostalCodeDoesNotExist() throws Exception {
        assertAddressError(
                new PostalCodeNotFoundException(),
                422,
                "POSTAL_CODE_NOT_FOUND"
        );
    }

    @Test
    void returnsServiceUnavailableWhenPostalCodeProviderFails() throws Exception {
        assertAddressError(
                new PostalCodeServiceUnavailableException(),
                503,
                "POSTAL_CODE_SERVICE_UNAVAILABLE"
        );
    }

    @Test
    void returnsForbiddenWhenSolicitationBelongsToAnotherClient() throws Exception {
        assertAddressError(
                new SolicitationAccessDeniedException(),
                403,
                "SOLICITATION_ACCESS_DENIED"
        );
    }

    @Test
    void returnsNotFoundWhenSolicitationDoesNotExist() throws Exception {
        assertAddressError(
                new SolicitationNotFoundException(),
                404,
                "SOLICITATION_NOT_FOUND"
        );
    }

    @Test
    void returnsConflictWhenSolicitationCannotBeEdited() throws Exception {
        assertAddressError(
                new InvalidSolicitationStateException("Only draft solicitations can be edited"),
                409,
                "INVALID_SOLICITATION_STATE"
        );
    }

    @Test
    void returnsConflictWhenSolicitationWasUpdatedConcurrently() throws Exception {
        assertAddressError(
                new SolicitationConcurrentUpdateException(),
                409,
                "SOLICITATION_CONCURRENT_UPDATE"
        );
    }

    private void assertAddressError(
            RuntimeException exception,
            int expectedStatus,
            String expectedCode
    ) throws Exception {
        UUID solicitationId = UUID.randomUUID();
        when(completeSolicitationAddressUseCase.complete(any(CompleteSolicitationAddressCommand.class)))
                .thenThrow(exception);

        mockMvc.perform(put("/solicitations/{id}/address", solicitationId)
                        .with(jwt()
                                .jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "postalCode": "01001000",
                                  "number": "100"
                                }
                                """))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.path")
                        .value("/solicitations/" + solicitationId + "/address"));
    }
}
