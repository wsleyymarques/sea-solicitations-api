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
