package br.com.sea.solicitations.solicitation.adapter.in.web;

import br.com.sea.solicitations.shared.adapter.in.web.error.GlobalExceptionHandler;
import br.com.sea.solicitations.shared.adapter.in.web.security.SecurityConfiguration;
import br.com.sea.solicitations.shared.adapter.in.web.security.SecurityErrorHandler;
import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.SearchUnavailableException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationReviewAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.port.in.AnalystSolicitationSummary;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.GetAnalystSolicitationQuery;
import br.com.sea.solicitations.solicitation.application.port.in.GetAnalystSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.in.GetAnalystSolicitationUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.ListAnalystSolicitationsQuery;
import br.com.sea.solicitations.solicitation.application.port.in.ListAnalystSolicitationsResult;
import br.com.sea.solicitations.solicitation.application.port.in.ListAnalystSolicitationsUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationSummary;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsQuery;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsResult;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSortField;
import br.com.sea.solicitations.solicitation.application.port.in.SearchDirection;
import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewCommand;
import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewResult;
import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewUseCase;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalystSolicitationController.class)
@Import({SecurityConfiguration.class, SecurityErrorHandler.class, GlobalExceptionHandler.class})
class AnalystSolicitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StartSolicitationReviewUseCase startUseCase;

    @MockitoBean
    private DecideSolicitationUseCase decideUseCase;

    @MockitoBean
    private ListAnalystSolicitationsUseCase listUseCase;

    @MockitoBean
    private SearchSolicitationsUseCase searchUseCase;

    @MockitoBean
    private GetAnalystSolicitationUseCase getUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getsCompleteSolicitationUsingAnalystIdentityAndCoverageFromJwt() throws Exception {
        UUID id = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        Instant submittedAt = Instant.parse("2026-11-02T12:00:00Z");
        when(getUseCase.get(any(GetAnalystSolicitationQuery.class))).thenReturn(
                new GetAnalystSolicitationResult(
                        id,
                        clientId,
                        new GetAnalystSolicitationResult.BasicDataDetails(
                                ServiceType.INSTALLATION,
                                "Instalacao de equipamento",
                                "Solicito a instalacao de um novo equipamento no endereco informado."
                        ),
                        new GetAnalystSolicitationResult.AddressDetails(
                                "01001000", "Praca da Se", "Se", "Sao Paulo",
                                FederativeUnit.SP, "100", "Apto 12"
                        ),
                        new GetAnalystSolicitationResult.ConfirmationDetails(
                                Priority.HIGH, LocalDate.of(2026, 11, 4),
                                new BigDecimal("100.00"), true
                        ),
                        SolicitationStatus.SUBMITTED,
                        SolicitationStep.COMPLETED,
                        null,
                        submittedAt,
                        null,
                        null,
                        Instant.parse("2026-11-01T10:00:00Z"),
                        Instant.parse("2026-11-02T12:00:00Z")
                )
        );

        mockMvc.perform(get("/analyst/solicitations/{id}", id)
                        .with(jwt().jwt(token -> token
                                        .subject(analystId.toString())
                                        .claim("coveredStates", List.of("SP", "RJ")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.basicData.serviceType").value("INSTALLATION"))
                .andExpect(jsonPath("$.basicData.title").value("Instalacao de equipamento"))
                .andExpect(jsonPath("$.address.state").value("SP"))
                .andExpect(jsonPath("$.address.complement").value("Apto 12"))
                .andExpect(jsonPath("$.confirmation.priority").value("HIGH"))
                .andExpect(jsonPath("$.confirmation.estimatedValue").value(100.00))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.currentStep").value("COMPLETED"))
                .andExpect(jsonPath("$.submittedAt").value(submittedAt.toString()));

        ArgumentCaptor<GetAnalystSolicitationQuery> captor =
                ArgumentCaptor.forClass(GetAnalystSolicitationQuery.class);
        verify(getUseCase).get(captor.capture());
        assertThat(captor.getValue().solicitationId()).isEqualTo(id);
        assertThat(captor.getValue().analystId()).isEqualTo(analystId);
        assertThat(captor.getValue().coveredStates()).containsExactlyInAnyOrder(
                FederativeUnit.SP, FederativeUnit.RJ
        );
    }

    @Test
    void protectsDetailAndRejectsMalformedCoverage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/analyst/solicitations/{id}", id))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/analyst/solicitations/{id}", id)
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/analyst/solicitations/{id}", id)
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SOLICITATION_REVIEW_ACCESS_DENIED"));

        verifyNoInteractions(getUseCase);
    }

    @Test
    void mapsDetailNotFoundAndAccessDeniedErrors() throws Exception {
        UUID id = UUID.randomUUID();
        var analystToken = jwt().jwt(token -> token
                        .subject(UUID.randomUUID().toString())
                        .claim("coveredStates", List.of("SP")))
                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"));
        when(getUseCase.get(any(GetAnalystSolicitationQuery.class)))
                .thenThrow(new SolicitationNotFoundException())
                .thenThrow(new SolicitationReviewAccessDeniedException());

        mockMvc.perform(get("/analyst/solicitations/{id}", id).with(analystToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SOLICITATION_NOT_FOUND"));

        mockMvc.perform(get("/analyst/solicitations/{id}", id).with(analystToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SOLICITATION_REVIEW_ACCESS_DENIED"));
    }

    @Test
    void searchesWithJwtCoverageAndFilters() throws Exception {
        UUID analystId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        when(searchUseCase.search(any(SearchSolicitationsQuery.class))).thenReturn(
                new SearchSolicitationsResult(List.of(new SearchSolicitationSummary(
                        solicitationId, SolicitationStatus.APPROVED, ServiceType.INSTALLATION,
                        "Instalacao de equipamento", Priority.HIGH, FederativeUnit.SP,
                        Instant.parse("2026-11-01T10:00:00Z"),
                        Instant.parse("2026-11-02T12:00:00Z"),
                        LocalDate.of(2026, 11, 4), analystId
                )), 1, 5, 6, 2)
        );

        mockMvc.perform(get("/analyst/solicitations/search")
                        .param("text", " equipamento ")
                        .param("status", "APPROVED")
                        .param("serviceType", "INSTALLATION")
                        .param("priority", "HIGH")
                        .param("state", "SP")
                        .param("dateFrom", "2026-11-01")
                        .param("dateTo", "2026-11-03")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "CREATED_AT")
                        .param("direction", "ASC")
                        .with(jwt().jwt(token -> token
                                        .subject(analystId.toString())
                                        .claim("coveredStates", List.of("SP", "RJ")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.content[0].id").value(solicitationId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.content[0].state").value("SP"));

        ArgumentCaptor<SearchSolicitationsQuery> captor =
                ArgumentCaptor.forClass(SearchSolicitationsQuery.class);
        verify(searchUseCase).search(captor.capture());
        assertThat(captor.getValue().analystId()).isEqualTo(analystId);
        assertThat(captor.getValue().coveredStates()).containsExactlyInAnyOrder(
                FederativeUnit.SP, FederativeUnit.RJ
        );
        assertThat(captor.getValue().text()).isEqualTo("equipamento");
        assertThat(captor.getValue().submittedTo()).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(captor.getValue().sortBy()).isEqualTo(SearchSortField.CREATED_AT);
        assertThat(captor.getValue().direction()).isEqualTo(SearchDirection.ASC);
    }

    @Test
    void acceptsLegacySearchParameterAliases() throws Exception {
        when(searchUseCase.search(any(SearchSolicitationsQuery.class))).thenReturn(
                new SearchSolicitationsResult(List.of(), 0, 20, 0, 0)
        );

        mockMvc.perform(get("/analyst/solicitations/search")
                        .param("submittedFrom", "2026-10-01")
                        .param("submittedTo", "2026-10-31")
                        .param("sortBy", "PREFERRED_DATE")
                        .with(jwt().jwt(token -> token
                                        .subject(UUID.randomUUID().toString())
                                        .claim("coveredStates", List.of("SP")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isOk());

        ArgumentCaptor<SearchSolicitationsQuery> captor =
                ArgumentCaptor.forClass(SearchSolicitationsQuery.class);
        verify(searchUseCase).search(captor.capture());
        assertThat(captor.getValue().submittedFrom()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(captor.getValue().submittedTo()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(captor.getValue().sortBy()).isEqualTo(SearchSortField.PREFERRED_DATE);
    }

    @Test
    void canonicalSearchParametersTakePrecedenceOverLegacyAliases() throws Exception {
        when(searchUseCase.search(any(SearchSolicitationsQuery.class))).thenReturn(
                new SearchSolicitationsResult(List.of(), 0, 20, 0, 0)
        );

        mockMvc.perform(get("/analyst/solicitations/search")
                        .param("dateFrom", "2026-11-05")
                        .param("submittedFrom", "2026-10-01")
                        .param("dateTo", "2026-11-06")
                        .param("submittedTo", "2026-10-31")
                        .param("sort", "SUBMITTED_AT")
                        .param("sortBy", "PREFERRED_DATE")
                        .with(jwt().jwt(token -> token
                                        .subject(UUID.randomUUID().toString())
                                        .claim("coveredStates", List.of("SP")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isOk());

        ArgumentCaptor<SearchSolicitationsQuery> captor =
                ArgumentCaptor.forClass(SearchSolicitationsQuery.class);
        verify(searchUseCase).search(captor.capture());
        assertThat(captor.getValue().submittedFrom()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(captor.getValue().submittedTo()).isEqualTo(LocalDate.of(2026, 11, 6));
        assertThat(captor.getValue().sortBy()).isEqualTo(SearchSortField.SUBMITTED_AT);
    }

    @Test
    void rejectsInvalidSearchPaginationAndClaim() throws Exception {
        var analystToken = jwt().jwt(token -> token
                        .subject(UUID.randomUUID().toString())
                        .claim("coveredStates", List.of("SP")))
                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"));

        mockMvc.perform(get("/analyst/solicitations/search")
                        .param("page", "500").param("size", "20")
                        .with(analystToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/analyst/solicitations/search")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/analyst/solicitations/search")
                        .with(jwt().jwt(token -> token
                                        .subject(UUID.randomUUID().toString())
                                        .claim("coveredStates", List.of("SP")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(searchUseCase);
    }

    @Test
    void reportsSearchEngineFailureAsServiceUnavailable() throws Exception {
        when(searchUseCase.search(any(SearchSolicitationsQuery.class)))
                .thenThrow(new SearchUnavailableException(new IllegalStateException("offline")));

        mockMvc.perform(get("/analyst/solicitations/search")
                        .with(jwt().jwt(token -> token
                                        .subject(UUID.randomUUID().toString())
                                        .claim("coveredStates", List.of("SP")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SEARCH_UNAVAILABLE"));
    }

    @Test
    void decidesUsingAnalystIdentityAndCoverageFromJwt() throws Exception {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        Instant decidedAt = Instant.parse("2026-11-02T12:00:00Z");
        when(decideUseCase.decide(any(DecideSolicitationCommand.class))).thenReturn(
                new DecideSolicitationResult(id, SolicitationStatus.APPROVED, analystId,
                        "Parecer favoravel para atendimento.", decidedAt)
        );

        mockMvc.perform(post("/analyst/solicitations/{id}/decide", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"APPROVED\",\"comment\":\"Parecer favoravel para atendimento.\"}")
                        .with(jwt().jwt(token -> token
                                        .subject(analystId.toString())
                                        .claim("coveredStates", List.of("SP", "RJ")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.analystId").value(analystId.toString()))
                .andExpect(jsonPath("$.comment").value("Parecer favoravel para atendimento."))
                .andExpect(jsonPath("$.decidedAt").value(decidedAt.toString()));

        ArgumentCaptor<DecideSolicitationCommand> captor =
                ArgumentCaptor.forClass(DecideSolicitationCommand.class);
        verify(decideUseCase).decide(captor.capture());
        assertThat(captor.getValue().solicitationId()).isEqualTo(id);
        assertThat(captor.getValue().analystId()).isEqualTo(analystId);
        assertThat(captor.getValue().coveredStates()).containsExactlyInAnyOrder(
                FederativeUnit.SP, FederativeUnit.RJ
        );
        assertThat(captor.getValue().outcome().name()).isEqualTo("APPROVED");
        assertThat(captor.getValue().comment()).isEqualTo("Parecer favoravel para atendimento.");
    }

    @Test
    void rejectsMissingOrInvalidDecisionBody() throws Exception {
        UUID id = UUID.randomUUID();
        var token = jwt().jwt(jwt -> jwt
                        .subject(UUID.randomUUID().toString())
                        .claim("coveredStates", List.of("SP")))
                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"));

        mockMvc.perform(post("/analyst/solicitations/{id}/decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"APPROVED\",\"comment\":\"short\"}")
                        .with(token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/analyst/solicitations/{id}/decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"SUBMITTED\",\"comment\":\"Comentario suficiente.\"}")
                        .with(token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));

        mockMvc.perform(post("/analyst/solicitations/{id}/decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/analyst/solicitations/{id}/decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"REJECTED\",\"comment\":\"             \"}")
                        .with(token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/analyst/solicitations/{id}/decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));

        verifyNoInteractions(decideUseCase);
    }

    @Test
    void trimsDecisionCommentBeforeSendingToUseCase() throws Exception {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        when(decideUseCase.decide(any(DecideSolicitationCommand.class))).thenReturn(
                new DecideSolicitationResult(id, SolicitationStatus.REJECTED, analystId,
                        "Documentacao insuficiente.", Instant.parse("2026-11-02T12:00:00Z"))
        );

        mockMvc.perform(post("/analyst/solicitations/{id}/decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"REJECTED\",\"comment\":\"  Documentacao insuficiente.  \"}")
                        .with(jwt().jwt(token -> token
                                        .subject(analystId.toString())
                                        .claim("coveredStates", List.of("SP")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        ArgumentCaptor<DecideSolicitationCommand> captor =
                ArgumentCaptor.forClass(DecideSolicitationCommand.class);
        verify(decideUseCase).decide(captor.capture());
        assertThat(captor.getValue().comment()).isEqualTo("Documentacao insuficiente.");
    }

    @Test
    void deniesDecisionWhenCoverageClaimIsMissingOrRoleIsClient() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{\"outcome\":\"REJECTED\",\"comment\":\"Documentacao insuficiente.\"}";

        mockMvc.perform(post("/analyst/solicitations/{id}/decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SOLICITATION_REVIEW_ACCESS_DENIED"));

        mockMvc.perform(post("/analyst/solicitations/{id}/decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().jwt(token -> token
                                        .subject(UUID.randomUUID().toString())
                                        .claim("coveredStates", List.of("SP")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(decideUseCase);
    }

    @Test
    void startsReviewUsingAnalystIdentityAndCoverageFromJwt() throws Exception {
        UUID id = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-11-02T12:00:00Z");
        when(startUseCase.start(any(StartSolicitationReviewCommand.class))).thenReturn(
                new StartSolicitationReviewResult(id, SolicitationStatus.IN_REVIEW, analystId, startedAt)
        );

        mockMvc.perform(post("/analyst/solicitations/{id}/start", id)
                        .with(jwt().jwt(token -> token
                                        .subject(analystId.toString())
                                        .claim("coveredStates", List.of("SP", "RJ")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.assignedAnalystId").value(analystId.toString()))
                .andExpect(jsonPath("$.reviewStartedAt").value(startedAt.toString()));

        ArgumentCaptor<StartSolicitationReviewCommand> captor =
                ArgumentCaptor.forClass(StartSolicitationReviewCommand.class);
        verify(startUseCase).start(captor.capture());
        assertThat(captor.getValue().solicitationId()).isEqualTo(id);
        assertThat(captor.getValue().analystId()).isEqualTo(analystId);
        assertThat(captor.getValue().coveredStates()).containsExactlyInAnyOrder(
                FederativeUnit.SP,
                FederativeUnit.RJ
        );
    }

    @Test
    void deniesAnalystWithMissingCoverageClaim() throws Exception {
        mockMvc.perform(post("/analyst/solicitations/{id}/start", UUID.randomUUID())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SOLICITATION_REVIEW_ACCESS_DENIED"));

        verifyNoInteractions(startUseCase);
    }

    @Test
    void deniesAnalystWithMalformedCoverageClaim() throws Exception {
        mockMvc.perform(post("/analyst/solicitations/{id}/start", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                        .subject(UUID.randomUUID().toString())
                                        .claim("coveredStates", List.of("XX")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SOLICITATION_REVIEW_ACCESS_DENIED"));

        verifyNoInteractions(startUseCase);
    }

    @Test
    void clientRoleCannotStartReview() throws Exception {
        mockMvc.perform(post("/analyst/solicitations/{id}/start", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                        .subject(UUID.randomUUID().toString())
                                        .claim("coveredStates", List.of("SP")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(startUseCase);
    }

    @Test
    void listsVisibleSolicitationsUsingIdentityCoverageAndRequestedPage() throws Exception {
        UUID solicitationId = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();
        Instant submittedAt = Instant.parse("2026-11-01T12:00:00Z");
        when(listUseCase.list(any(ListAnalystSolicitationsQuery.class))).thenReturn(
                new ListAnalystSolicitationsResult(
                        List.of(new AnalystSolicitationSummary(
                                solicitationId,
                                SolicitationStatus.SUBMITTED,
                                ServiceType.INSTALLATION,
                                "Instalacao de equipamento",
                                Priority.HIGH,
                                FederativeUnit.SP,
                                submittedAt,
                                null,
                                null
                        )),
                        1,
                        5,
                        6,
                        2
                )
        );

        mockMvc.perform(get("/analyst/solicitations")
                        .param("page", "1")
                        .param("size", "5")
                        .with(jwt().jwt(token -> token
                                        .subject(analystId.toString())
                                        .claim("coveredStates", List.of("SP", "RJ")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(solicitationId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.content[0].serviceType").value("INSTALLATION"))
                .andExpect(jsonPath("$.content[0].title").value("Instalacao de equipamento"))
                .andExpect(jsonPath("$.content[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.content[0].state").value("SP"))
                .andExpect(jsonPath("$.content[0].submittedAt").value(submittedAt.toString()))
                .andExpect(jsonPath("$.content[0].assignedAnalystId").doesNotExist())
                .andExpect(jsonPath("$.content[0].reviewStartedAt").doesNotExist());

        ArgumentCaptor<ListAnalystSolicitationsQuery> captor =
                ArgumentCaptor.forClass(ListAnalystSolicitationsQuery.class);
        verify(listUseCase).list(captor.capture());
        assertThat(captor.getValue().analystId()).isEqualTo(analystId);
        assertThat(captor.getValue().coveredStates()).containsExactlyInAnyOrder(
                FederativeUnit.SP,
                FederativeUnit.RJ
        );
        assertThat(captor.getValue().page()).isEqualTo(1);
        assertThat(captor.getValue().size()).isEqualTo(5);
    }

    @Test
    void usesDefaultPaginationWhenParametersAreOmitted() throws Exception {
        when(listUseCase.list(any(ListAnalystSolicitationsQuery.class))).thenReturn(
                new ListAnalystSolicitationsResult(List.of(), 0, 20, 0, 0)
        );

        mockMvc.perform(get("/analyst/solicitations")
                        .with(jwt().jwt(token -> token
                                        .subject(UUID.randomUUID().toString())
                                        .claim("coveredStates", List.of("SP")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content").isEmpty());

        ArgumentCaptor<ListAnalystSolicitationsQuery> captor =
                ArgumentCaptor.forClass(ListAnalystSolicitationsQuery.class);
        verify(listUseCase).list(captor.capture());
        assertThat(captor.getValue().page()).isZero();
        assertThat(captor.getValue().size()).isEqualTo(20);
    }

    @Test
    void rejectsInvalidListPagination() throws Exception {
        mockMvc.perform(get("/analyst/solicitations")
                        .param("page", "-1")
                        .with(jwt().jwt(token -> token
                                        .subject(UUID.randomUUID().toString())
                                        .claim("coveredStates", List.of("SP")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/analyst/solicitations")
                        .param("size", "101")
                        .with(jwt().jwt(token -> token
                                        .subject(UUID.randomUUID().toString())
                                        .claim("coveredStates", List.of("SP")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(listUseCase);
    }

    @Test
    void deniesListWhenCoverageClaimIsMissing() throws Exception {
        mockMvc.perform(get("/analyst/solicitations")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SOLICITATION_REVIEW_ACCESS_DENIED"));

        verifyNoInteractions(listUseCase);
    }

    @Test
    void clientRoleCannotListAnalystSolicitations() throws Exception {
        mockMvc.perform(get("/analyst/solicitations")
                        .with(jwt().jwt(token -> token
                                        .subject(UUID.randomUUID().toString())
                                        .claim("coveredStates", List.of("SP")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(listUseCase);
    }
}
