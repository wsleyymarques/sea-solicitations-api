package br.com.sea.solicitations.user.adapter.in.web;

import br.com.sea.solicitations.user.adapter.in.web.dto.CreateInternalUserRequest;
import br.com.sea.solicitations.user.adapter.in.web.dto.CreateInternalUserResponse;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserCommand;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserResult;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserUseCase;
import br.com.sea.solicitations.shared.adapter.in.web.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/admin/users")
@Tag(name = "Administracao de usuarios")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente ou invalido",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Papel insuficiente",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AdminUserController {

    private final CreateInternalUserUseCase useCase;

    public AdminUserController(CreateInternalUserUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "Create internal user use case is required");
    }

    @PostMapping
    @Operation(summary = "Criar analista ou administrador")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario criado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso reservado a administradores"),
            @ApiResponse(responseCode = "409", description = "Email ja cadastrado")
    })
    public ResponseEntity<CreateInternalUserResponse> create(
            @Valid @RequestBody CreateInternalUserRequest request
    ) {
        CreateInternalUserCommand command = new CreateInternalUserCommand(
                request.name(),
                request.email(),
                request.password(),
                request.role(),
                request.coveredStates() == null ? Set.of() : request.coveredStates()
        );
        CreateInternalUserResult result = useCase.create(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CreateInternalUserResponse.from(result));
    }
}
