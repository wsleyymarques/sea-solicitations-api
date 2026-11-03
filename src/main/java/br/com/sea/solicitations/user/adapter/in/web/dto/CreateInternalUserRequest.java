package br.com.sea.solicitations.user.adapter.in.web.dto;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.user.domain.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateInternalUserRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must have at most 120 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 180, message = "Email must have at most 180 characters")
        String email,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must have between 8 and 72 characters")
        String password,

        @NotNull(message = "Role is required")
        UserRole role,

        Set<@NotNull(message = "Covered states cannot contain null values") FederativeUnit> coveredStates
) {

    @Override
    public String toString() {
        return "CreateInternalUserRequest["
                + "name=" + name
                + ", email=" + email
                + ", password=[REDACTED]"
                + ", role=" + role
                + ", coveredStates=" + coveredStates
                + "]";
    }
}
