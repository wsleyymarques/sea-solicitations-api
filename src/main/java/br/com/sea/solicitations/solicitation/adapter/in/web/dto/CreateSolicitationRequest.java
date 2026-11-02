package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.solicitation.domain.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSolicitationRequest(
        @NotNull(message = "Service type is required")
        ServiceType serviceType,

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 80, message = "Title must have between 3 and 80 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(min = 20, max = 1000, message = "Description must have between 20 and 1000 characters")
        String description
) {
}
