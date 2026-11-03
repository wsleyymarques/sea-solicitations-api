package br.com.sea.solicitations.solicitation.adapter.in.web;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.adapter.in.web.dto.DecideSolicitationRequest;
import br.com.sea.solicitations.solicitation.adapter.in.web.dto.DecideSolicitationResponse;
import br.com.sea.solicitations.solicitation.adapter.in.web.dto.ListAnalystSolicitationsResponse;
import br.com.sea.solicitations.solicitation.adapter.in.web.dto.SearchSolicitationsResponse;
import br.com.sea.solicitations.solicitation.adapter.in.web.dto.StartSolicitationReviewResponse;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationReviewAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.ListAnalystSolicitationsQuery;
import br.com.sea.solicitations.solicitation.application.port.in.ListAnalystSolicitationsResult;
import br.com.sea.solicitations.solicitation.application.port.in.ListAnalystSolicitationsUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.SearchDirection;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsQuery;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsResult;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSortField;
import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewCommand;
import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewResult;
import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewUseCase;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.shared.adapter.in.web.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/analyst/solicitations")
@Tag(name = "Analise de solicitacoes")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente ou invalido",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Papel ou acesso insuficiente",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AnalystSolicitationController {

    private final ListAnalystSolicitationsUseCase listAnalystSolicitationsUseCase;
    private final StartSolicitationReviewUseCase startSolicitationReviewUseCase;
    private final DecideSolicitationUseCase decideSolicitationUseCase;
    private final SearchSolicitationsUseCase searchSolicitationsUseCase;

    public AnalystSolicitationController(
            ListAnalystSolicitationsUseCase listAnalystSolicitationsUseCase,
            StartSolicitationReviewUseCase startSolicitationReviewUseCase,
            DecideSolicitationUseCase decideSolicitationUseCase,
            SearchSolicitationsUseCase searchSolicitationsUseCase
    ) {
        this.listAnalystSolicitationsUseCase = Objects.requireNonNull(
                listAnalystSolicitationsUseCase, "List analyst solicitations use case is required"
        );
        this.startSolicitationReviewUseCase = Objects.requireNonNull(
                startSolicitationReviewUseCase, "Start solicitation review use case is required"
        );
        this.decideSolicitationUseCase = Objects.requireNonNull(
                decideSolicitationUseCase, "Decide solicitation use case is required"
        );
        this.searchSolicitationsUseCase = Objects.requireNonNull(
                searchSolicitationsUseCase, "Search solicitations use case is required"
        );
    }

    @GetMapping
    @Operation(summary = "Listar solicitacoes visiveis ao analista")
    @ApiResponse(responseCode = "400", description = "Paginacao invalida",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ListAnalystSolicitationsResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ListAnalystSolicitationsQuery query = new ListAnalystSolicitationsQuery(
                UUID.fromString(jwt.getSubject()),
                coveredStates(jwt),
                page,
                size
        );
        ListAnalystSolicitationsResult result = listAnalystSolicitationsUseCase.list(query);
        return ResponseEntity.ok(ListAnalystSolicitationsResponse.from(result));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar solicitacoes no Elasticsearch")
    @ApiResponse(responseCode = "400", description = "Filtros invalidos",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Busca indisponivel")
    public ResponseEntity<SearchSolicitationsResponse> search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) SolicitationStatus status,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) FederativeUnit state,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate submittedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate submittedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "SUBMITTED_AT") SearchSortField sortBy,
            @RequestParam(defaultValue = "DESC") SearchDirection direction
    ) {
        SearchSolicitationsQuery query = new SearchSolicitationsQuery(
                UUID.fromString(jwt.getSubject()), coveredStates(jwt), text, status, serviceType,
                priority, state, submittedFrom, submittedTo, page, size, sortBy, direction
        );
        SearchSolicitationsResult result = searchSolicitationsUseCase.search(query);
        return ResponseEntity.ok(SearchSolicitationsResponse.from(result));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Iniciar analise de solicitacao")
    @ApiResponse(responseCode = "403", description = "Analista sem acesso a solicitacao")
    @ApiResponse(responseCode = "404", description = "Solicitacao nao encontrada")
    @ApiResponse(responseCode = "409", description = "Estado invalido ou atualizacao concorrente")
    public ResponseEntity<StartSolicitationReviewResponse> startReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        StartSolicitationReviewCommand command = new StartSolicitationReviewCommand(
                id,
                UUID.fromString(jwt.getSubject()),
                coveredStates(jwt)
        );
        StartSolicitationReviewResult result = startSolicitationReviewUseCase.start(command);
        return ResponseEntity.ok(StartSolicitationReviewResponse.from(result));
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "Aprovar ou rejeitar solicitacao")
    @ApiResponse(responseCode = "400", description = "Decisao invalida",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Analista sem acesso a solicitacao")
    @ApiResponse(responseCode = "404", description = "Solicitacao nao encontrada")
    @ApiResponse(responseCode = "409", description = "Estado invalido ou atualizacao concorrente")
    public ResponseEntity<DecideSolicitationResponse> decide(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DecideSolicitationRequest request
    ) {
        DecideSolicitationCommand command = new DecideSolicitationCommand(
                id,
                UUID.fromString(jwt.getSubject()),
                coveredStates(jwt),
                request.outcome(),
                request.comment()
        );
        DecideSolicitationResult result = decideSolicitationUseCase.decide(command);
        return ResponseEntity.ok(DecideSolicitationResponse.from(result));
    }

    private static Set<FederativeUnit> coveredStates(Jwt jwt) {
        Object claim = jwt.getClaim("coveredStates");
        if (!(claim instanceof List<?> values)) {
            throw new SolicitationReviewAccessDeniedException();
        }

        Set<FederativeUnit> states = new HashSet<>();
        for (Object value : values) {
            if (!(value instanceof String state)) {
                throw new SolicitationReviewAccessDeniedException();
            }
            try {
                states.add(FederativeUnit.valueOf(state));
            } catch (IllegalArgumentException exception) {
                throw new SolicitationReviewAccessDeniedException();
            }
        }
        return states;
    }
}
