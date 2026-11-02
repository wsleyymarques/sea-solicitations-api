package br.com.sea.solicitations.solicitation.adapter.in.web;

import br.com.sea.solicitations.shared.adapter.in.web.error.GlobalExceptionHandler;
import br.com.sea.solicitations.shared.adapter.in.web.security.SecurityConfiguration;
import br.com.sea.solicitations.shared.adapter.in.web.security.SecurityErrorHandler;
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
}
