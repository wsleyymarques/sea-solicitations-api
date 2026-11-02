package br.com.sea.solicitations.solicitation.domain;

import br.com.sea.solicitations.solicitation.domain.exception.IncompleteSolicitationException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "solicitations")
public class Solicitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Embedded
    private BasicData basicData;

    @Embedded
    private Address address;

    @Embedded
    private Confirmation confirmation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SolicitationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false, length = 20)
    private SolicitationStep currentStep;

    @Column(name = "assigned_analyst_id")
    private UUID assignedAnalystId;

    @Embedded
    private Decision decision;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "review_started_at")
    private Instant reviewStartedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Solicitation() {
    }

    public Solicitation(UUID clientId) {
        this.clientId = Objects.requireNonNull(clientId, "Client is required");
        this.status = SolicitationStatus.DRAFT;
        this.currentStep = SolicitationStep.BASIC_DATA;
    }

    public void completeBasicData(BasicData basicData) {
        requireDraft();
        this.basicData = Objects.requireNonNull(basicData, "Basic data is required");
        if (currentStep == SolicitationStep.BASIC_DATA) {
            currentStep = SolicitationStep.ADDRESS;
        }
    }

    public void completeAddress(Address address) {
        requireDraft();
        if (basicData == null) {
            throw new IllegalStateException("Basic data must be completed before the address");
        }
        this.address = Objects.requireNonNull(address, "Address is required");
        if (currentStep == SolicitationStep.ADDRESS) {
            currentStep = SolicitationStep.CONFIRMATION;
        }
    }

    public void completeConfirmation(Confirmation confirmation, LocalDate currentDate) {
        requireDraft();
        if (basicData == null || address == null) {
            throw new IllegalStateException("Basic data and address must be completed before confirmation");
        }
        Confirmation validConfirmation = Objects.requireNonNull(confirmation, "Confirmation is required");
        validConfirmation.validateFor(currentDate);
        this.confirmation = validConfirmation;
        currentStep = SolicitationStep.COMPLETED;
    }

    public void submit(LocalDate currentDate, Instant submittedAt) {
        requireDraft();
        List<SolicitationStep> missingSteps = new ArrayList<>(3);
        if (basicData == null) {
            missingSteps.add(SolicitationStep.BASIC_DATA);
        }
        if (address == null) {
            missingSteps.add(SolicitationStep.ADDRESS);
        }
        if (confirmation == null) {
            missingSteps.add(SolicitationStep.CONFIRMATION);
        }
        if (!missingSteps.isEmpty()) {
            throw new IncompleteSolicitationException(missingSteps);
        }
        confirmation.validateFor(currentDate);
        this.submittedAt = Objects.requireNonNull(submittedAt, "Submission time is required");
        status = SolicitationStatus.SUBMITTED;
    }

    public void startReview(UUID analystId, Instant startedAt) {
        requireStatus(SolicitationStatus.SUBMITTED);
        assignedAnalystId = Objects.requireNonNull(analystId, "Analyst is required");
        reviewStartedAt = Objects.requireNonNull(startedAt, "Review start time is required");
        status = SolicitationStatus.IN_REVIEW;
    }

    public void approve(UUID analystId, String comment, Instant decidedAt) {
        decide(SolicitationStatus.APPROVED, analystId, comment, decidedAt);
    }

    public void reject(UUID analystId, String comment, Instant decidedAt) {
        decide(SolicitationStatus.REJECTED, analystId, comment, decidedAt);
    }

    public boolean isOwnedBy(UUID userId) {
        return clientId.equals(userId);
    }

    private void decide(
            SolicitationStatus targetStatus,
            UUID analystId,
            String comment,
            Instant decidedAt
    ) {
        requireStatus(SolicitationStatus.IN_REVIEW);
        if (!Objects.equals(assignedAnalystId, analystId)) {
            throw new IllegalStateException("Only the assigned analyst can decide this solicitation");
        }
        decision = new Decision(analystId, comment, decidedAt);
        status = targetStatus;
    }

    private void requireDraft() {
        requireStatus(SolicitationStatus.DRAFT);
    }

    private void requireStatus(SolicitationStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException(
                    "Solicitation must be " + expectedStatus + " but is " + status
            );
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public BasicData getBasicData() {
        return basicData;
    }

    public Address getAddress() {
        return address;
    }

    public Confirmation getConfirmation() {
        return confirmation;
    }

    public SolicitationStatus getStatus() {
        return status;
    }

    public SolicitationStep getCurrentStep() {
        return currentStep;
    }

    public UUID getAssignedAnalystId() {
        return assignedAnalystId;
    }

    public Decision getDecision() {
        return decision;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getReviewStartedAt() {
        return reviewStartedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
