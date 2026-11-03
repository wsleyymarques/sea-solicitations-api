package br.com.sea.solicitations.user.adapter.in.web;

import br.com.sea.solicitations.user.adapter.in.web.dto.LoginRequest;
import br.com.sea.solicitations.user.adapter.in.web.dto.LoginResponse;
import br.com.sea.solicitations.user.adapter.in.web.dto.RegisterClientRequest;
import br.com.sea.solicitations.user.adapter.in.web.dto.RegisterClientResponse;
import br.com.sea.solicitations.user.application.port.in.AuthenticateUserUseCase;
import br.com.sea.solicitations.user.application.port.in.AuthenticationCommand;
import br.com.sea.solicitations.user.application.port.in.AuthenticationResult;
import br.com.sea.solicitations.user.application.port.in.RegisterClientCommand;
import br.com.sea.solicitations.user.application.port.in.RegisterClientResult;
import br.com.sea.solicitations.user.application.port.in.RegisterClientUseCase;
import br.com.sea.solicitations.shared.adapter.in.web.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacao")
public class AuthController {

    private final RegisterClientUseCase registerClientUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;

    public AuthController(
            RegisterClientUseCase registerClientUseCase,
            AuthenticateUserUseCase authenticateUserUseCase
    ) {
        this.registerClientUseCase = Objects.requireNonNull(
                registerClientUseCase,
                "Register client use case is required"
        );
        this.authenticateUserUseCase = Objects.requireNonNull(
                authenticateUserUseCase,
                "Authenticate user use case is required"
        );
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastrar cliente")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = RegisterClientRequest.class),
            examples = @ExampleObject(name = "cliente", value = """
                    {"name":"Joao Souza","email":"joao@example.com","password":"StrongPassword@123"}
                    """)))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email ja cadastrado")
    })
    public ResponseEntity<RegisterClientResponse> register(
            @Valid @RequestBody RegisterClientRequest request
    ) {
        RegisterClientCommand command = new RegisterClientCommand(
                request.name(),
                request.email(),
                request.password()
        );
        RegisterClientResult result = registerClientUseCase.register(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RegisterClientResponse.from(result));
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuario")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = LoginRequest.class),
            examples = @ExampleObject(name = "credenciais", value = """
                    {"email":"joao@example.com","password":"StrongPassword@123"}
                    """)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token de acesso emitido"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas")
    })
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthenticationCommand command = new AuthenticationCommand(
                request.email(),
                request.password()
        );
        AuthenticationResult result = authenticateUserUseCase.authenticate(command);

        return ResponseEntity.ok(LoginResponse.from(result));
    }
}
