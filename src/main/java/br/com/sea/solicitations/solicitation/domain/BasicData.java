package br.com.sea.solicitations.solicitation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

@Embeddable
public class BasicData {

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", length = 30)
    private ServiceType serviceType;

    @Column(length = 80)
    private String title;

    @Column(length = 1000)
    private String description;

    protected BasicData() {
    }

    public BasicData(ServiceType serviceType, String title, String description) {
        this.serviceType = Objects.requireNonNull(serviceType, "Service type is required");
        this.title = requireLength(title, "Title", 3, 80);
        this.description = requireLength(description, "Description", 20, 1000);
    }

    private static String requireLength(String value, String field, int minimum, int maximum) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() < minimum || normalized.length() > maximum) {
            throw new IllegalArgumentException(
                    field + " must have between " + minimum + " and " + maximum + " characters"
            );
        }
        return normalized;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof BasicData that)) {
            return false;
        }
        return serviceType == that.serviceType
                && Objects.equals(title, that.title)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceType, title, description);
    }
}
