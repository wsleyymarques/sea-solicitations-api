package br.com.sea.solicitations.solicitation.adapter.in.web;

import br.com.sea.solicitations.solicitation.adapter.in.web.dto.CreateSolicitationRequest;
import br.com.sea.solicitations.solicitation.adapter.in.web.dto.CreateSolicitationResponse;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/solicitations")
public class SolicitationController {

    private final CreateSolicitationUseCase createSolicitationUseCase;

    public SolicitationController(CreateSolicitationUseCase createSolicitationUseCase) {
        this.createSolicitationUseCase = Objects.requireNonNull(
                createSolicitationUseCase,
                "Create solicitation use case is required"
        );
    }

    @PostMapping
    public ResponseEntity<CreateSolicitationResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateSolicitationRequest request
    ) {
        UUID clientId = UUID.fromString(jwt.getSubject());
        CreateSolicitationCommand command = new CreateSolicitationCommand(
                clientId,
                request.serviceType(),
                request.title(),
                request.description()
        );
        CreateSolicitationResult result = createSolicitationUseCase.create(command);
        URI location = URI.create("/solicitations/" + result.id());

        return ResponseEntity
                .created(location)
                .body(CreateSolicitationResponse.from(result));
    }
}
