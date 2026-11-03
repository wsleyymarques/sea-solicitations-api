package br.com.sea.solicitations.solicitation.adapter.in.web.dto;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.port.in.GetAnalystSolicitationResult;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GetAnalystSolicitationResponse(
        UUID id,
        UUID clientId,
        BasicDataResponse basicData,
        AddressResponse address,
        ConfirmationResponse confirmation,
        SolicitationStatus status,
        SolicitationStep currentStep,
        UUID assignedAnalystId,
        Instant submittedAt,
        Instant reviewStartedAt,
        DecisionResponse decision,
        Instant createdAt,
        Instant updatedAt
) {
    public static GetAnalystSolicitationResponse from(GetAnalystSolicitationResult result) {
        return new GetAnalystSolicitationResponse(
                result.id(),
                result.clientId(),
                BasicDataResponse.from(result.basicData()),
                AddressResponse.from(result.address()),
                ConfirmationResponse.from(result.confirmation()),
                result.status(),
                result.currentStep(),
                result.assignedAnalystId(),
                result.submittedAt(),
                result.reviewStartedAt(),
                DecisionResponse.from(result.decision()),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public record BasicDataResponse(ServiceType serviceType, String title, String description) {
        static BasicDataResponse from(GetAnalystSolicitationResult.BasicDataDetails details) {
            return new BasicDataResponse(details.serviceType(), details.title(), details.description());
        }
    }

    public record AddressResponse(
            String postalCode,
            String street,
            String neighborhood,
            String city,
            FederativeUnit state,
            String number,
            String complement
    ) {
        static AddressResponse from(GetAnalystSolicitationResult.AddressDetails details) {
            return new AddressResponse(
                    details.postalCode(), details.street(), details.neighborhood(), details.city(),
                    details.state(), details.number(), details.complement()
            );
        }
    }

    public record ConfirmationResponse(
            Priority priority,
            LocalDate preferredDate,
            BigDecimal estimatedValue,
            boolean termsAccepted
    ) {
        static ConfirmationResponse from(GetAnalystSolicitationResult.ConfirmationDetails details) {
            return new ConfirmationResponse(
                    details.priority(), details.preferredDate(), details.estimatedValue(),
                    details.termsAccepted()
            );
        }
    }

    public record DecisionResponse(UUID analystId, String comment, Instant decidedAt) {
        static DecisionResponse from(GetAnalystSolicitationResult.DecisionDetails details) {
            return details == null ? null : new DecisionResponse(
                    details.analystId(), details.comment(), details.decidedAt()
            );
        }
    }
}
