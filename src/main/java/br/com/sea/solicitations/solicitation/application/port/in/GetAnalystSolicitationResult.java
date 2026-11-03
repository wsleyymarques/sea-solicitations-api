package br.com.sea.solicitations.solicitation.application.port.in;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.domain.Address;
import br.com.sea.solicitations.solicitation.domain.BasicData;
import br.com.sea.solicitations.solicitation.domain.Confirmation;
import br.com.sea.solicitations.solicitation.domain.Decision;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record GetAnalystSolicitationResult(
        UUID id,
        UUID clientId,
        BasicDataDetails basicData,
        AddressDetails address,
        ConfirmationDetails confirmation,
        SolicitationStatus status,
        SolicitationStep currentStep,
        UUID assignedAnalystId,
        Instant submittedAt,
        Instant reviewStartedAt,
        DecisionDetails decision,
        Instant createdAt,
        Instant updatedAt
) {
    public GetAnalystSolicitationResult {
        Objects.requireNonNull(id, "Solicitation ID is required");
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(basicData, "Basic data is required");
        Objects.requireNonNull(address, "Address is required");
        Objects.requireNonNull(confirmation, "Confirmation is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(currentStep, "Current step is required");
        Objects.requireNonNull(submittedAt, "Submission time is required");
        Objects.requireNonNull(createdAt, "Creation time is required");
        Objects.requireNonNull(updatedAt, "Update time is required");
    }

    public static GetAnalystSolicitationResult from(Solicitation solicitation) {
        Objects.requireNonNull(solicitation, "Solicitation is required");
        return new GetAnalystSolicitationResult(
                solicitation.getId(),
                solicitation.getClientId(),
                BasicDataDetails.from(solicitation.getBasicData()),
                AddressDetails.from(solicitation.getAddress()),
                ConfirmationDetails.from(solicitation.getConfirmation()),
                solicitation.getStatus(),
                solicitation.getCurrentStep(),
                solicitation.getAssignedAnalystId(),
                solicitation.getSubmittedAt(),
                solicitation.getReviewStartedAt(),
                DecisionDetails.from(solicitation.getDecision()),
                solicitation.getCreatedAt(),
                solicitation.getUpdatedAt()
        );
    }

    public record BasicDataDetails(ServiceType serviceType, String title, String description) {
        static BasicDataDetails from(BasicData basicData) {
            Objects.requireNonNull(basicData, "Basic data is required");
            return new BasicDataDetails(
                    basicData.getServiceType(), basicData.getTitle(), basicData.getDescription()
            );
        }
    }

    public record AddressDetails(
            String postalCode,
            String street,
            String neighborhood,
            String city,
            FederativeUnit state,
            String number,
            String complement
    ) {
        static AddressDetails from(Address address) {
            Objects.requireNonNull(address, "Address is required");
            return new AddressDetails(
                    address.getPostalCode(), address.getStreet(), address.getNeighborhood(),
                    address.getCity(), address.getState(), address.getNumber(), address.getComplement()
            );
        }
    }

    public record ConfirmationDetails(
            Priority priority,
            LocalDate preferredDate,
            BigDecimal estimatedValue,
            boolean termsAccepted
    ) {
        static ConfirmationDetails from(Confirmation confirmation) {
            Objects.requireNonNull(confirmation, "Confirmation is required");
            return new ConfirmationDetails(
                    confirmation.getPriority(), confirmation.getPreferredDate(),
                    confirmation.getEstimatedValue(), confirmation.isTermsAccepted()
            );
        }
    }

    public record DecisionDetails(UUID analystId, String comment, Instant decidedAt) {
        static DecisionDetails from(Decision decision) {
            return decision == null ? null : new DecisionDetails(
                    decision.getAnalystId(), decision.getComment(), decision.getDecidedAt()
            );
        }
    }
}
