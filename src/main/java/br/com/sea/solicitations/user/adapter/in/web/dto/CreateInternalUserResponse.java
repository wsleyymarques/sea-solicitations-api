package br.com.sea.solicitations.user.adapter.in.web.dto;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserResult;
import br.com.sea.solicitations.user.domain.UserRole;

import java.util.Set;
import java.util.UUID;

public record CreateInternalUserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        Set<FederativeUnit> coveredStates
) {

    public static CreateInternalUserResponse from(CreateInternalUserResult result) {
        return new CreateInternalUserResponse(
                result.id(),
                result.name(),
                result.email(),
                result.role(),
                result.coveredStates()
        );
    }
}
