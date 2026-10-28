package br.com.sea.solicitations.solicitation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class Confirmation {

    private static final BigDecimal HIGH_PRIORITY_MINIMUM_VALUE = new BigDecimal("100.00");

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Priority priority;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(name = "estimated_value", precision = 19, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "terms_accepted")
    private Boolean termsAccepted;

    protected Confirmation() {
    }

    public Confirmation(
            Priority priority,
            LocalDate preferredDate,
            BigDecimal estimatedValue,
            boolean termsAccepted
    ) {
        this.priority = Objects.requireNonNull(priority, "Priority is required");
        this.preferredDate = Objects.requireNonNull(preferredDate, "Preferred date is required");
        this.estimatedValue = validateEstimatedValue(priority, estimatedValue);
        if (!termsAccepted) {
            throw new IllegalArgumentException("Terms must be accepted");
        }
        this.termsAccepted = true;
    }

    public void validateFor(LocalDate currentDate) {
        Objects.requireNonNull(currentDate, "Current date is required");
        if (preferredDate.isBefore(currentDate)) {
            throw new IllegalArgumentException("Preferred date cannot be in the past");
        }
    }

    private static BigDecimal validateEstimatedValue(Priority priority, BigDecimal estimatedValue) {
        if (estimatedValue != null && estimatedValue.signum() < 0) {
            throw new IllegalArgumentException("Estimated value cannot be negative");
        }
        if (priority == Priority.HIGH
                && (estimatedValue == null || estimatedValue.compareTo(HIGH_PRIORITY_MINIMUM_VALUE) < 0)) {
            throw new IllegalArgumentException("High priority requires an estimated value of at least 100.00");
        }
        return estimatedValue;
    }

    public Priority getPriority() {
        return priority;
    }

    public LocalDate getPreferredDate() {
        return preferredDate;
    }

    public BigDecimal getEstimatedValue() {
        return estimatedValue;
    }

    public boolean isTermsAccepted() {
        return Boolean.TRUE.equals(termsAccepted);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Confirmation that)) {
            return false;
        }
        return priority == that.priority
                && Objects.equals(preferredDate, that.preferredDate)
                && Objects.equals(estimatedValue, that.estimatedValue)
                && Objects.equals(termsAccepted, that.termsAccepted);
    }

    @Override
    public int hashCode() {
        return Objects.hash(priority, preferredDate, estimatedValue, termsAccepted);
    }
}
