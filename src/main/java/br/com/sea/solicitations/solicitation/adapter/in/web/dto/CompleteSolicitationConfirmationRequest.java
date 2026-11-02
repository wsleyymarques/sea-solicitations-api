package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.solicitation.domain.Priority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompleteSolicitationConfirmationRequest(
        @NotNull(message = "Priority is required")
        Priority priority,

        @NotNull(message = "Preferred date is required")
        @FutureOrPresent(message = "Preferred date must be today or in the future")
        LocalDate preferredDate,

        @PositiveOrZero(message = "Estimated value must be greater than or equal to zero")
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Estimated value must have at most 17 integer and 2 decimal digits"
        )
        BigDecimal estimatedValue,

        @NotNull(message = "Terms acceptance is required")
        @AssertTrue(message = "Terms must be accepted")
        Boolean termsAccepted
) {
}
