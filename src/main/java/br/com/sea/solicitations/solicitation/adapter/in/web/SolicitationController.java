package br.com.sea.solicitations.solicitation.adapter.in.web;

import br.com.sea.solicitations.solicitation.adapter.in.web.dto.CreateSolicitationRequest;
import br.com.sea.solicitations.solicitation.adapter.in.web.dto.CreateSolicitationResponse;
import br.com.sea.solicitations.solicitation.adapter.in.web.dto.CompleteSolicitationAddressRequest;
import br.com.sea.solicitations.solicitation.adapter.in.web.dto.CompleteSolicitationAddressResponse;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressResult;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/solicitations")
public class SolicitationController {

    private final CreateSolicitationUseCase createSolicitationUseCase;
    private final CompleteSolicitationAddressUseCase completeSolicitationAddressUseCase;

    public SolicitationController(
            CreateSolicitationUseCase createSolicitationUseCase,
            CompleteSolicitationAddressUseCase completeSolicitationAddressUseCase
    ) {
        this.createSolicitationUseCase = Objects.requireNonNull(
                createSolicitationUseCase,
                "Create solicitation use case is required"
        );
        this.completeSolicitationAddressUseCase = Objects.requireNonNull(
                completeSolicitationAddressUseCase,
                "Complete solicitation address use case is required"
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

    @PutMapping("/{id}/address")
    public ResponseEntity<CompleteSolicitationAddressResponse> completeAddress(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CompleteSolicitationAddressRequest request
    ) {
        CompleteSolicitationAddressCommand command = new CompleteSolicitationAddressCommand(
                id,
                UUID.fromString(jwt.getSubject()),
                request.postalCode(),
                request.number(),
                request.complement()
        );
        CompleteSolicitationAddressResult result = completeSolicitationAddressUseCase.complete(command);

        return ResponseEntity.ok(CompleteSolicitationAddressResponse.from(result));
    }
}
