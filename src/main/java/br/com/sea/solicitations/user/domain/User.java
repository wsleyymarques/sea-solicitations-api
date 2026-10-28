package br.com.sea.solicitations.user.domain;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "analyst_coverages",
            joinColumns = @JoinColumn(name = "analyst_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_analyst_coverages_analyst_state",
                    columnNames = {"analyst_id", "state"}
            )
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 2)
    private Set<FederativeUnit> assignedStates = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected User() {
    }

    public User(String name, String email, String passwordHash, UserRole role) {
        this.name = requireText(name, "Name", 120);
        this.email = normalizeEmail(email);
        this.passwordHash = requireText(passwordHash, "Password hash", 255);
        this.role = Objects.requireNonNull(role, "Role is required");
        this.active = true;
    }

    public void assignStates(Set<FederativeUnit> states) {
        if (role != UserRole.ANALYST) {
            throw new IllegalStateException("Only analysts can be assigned federative units");
        }
        Objects.requireNonNull(states, "Assigned states are required");
        if (states.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Assigned states cannot contain null values");
        }

        assignedStates.clear();
        assignedStates.addAll(states);
    }

    public boolean canReview(FederativeUnit state) {
        return role == UserRole.ANALYST && assignedStates.contains(state);
    }

    public void deactivate() {
        active = false;
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

    private static String normalizeEmail(String email) {
        return requireText(email, "Email", 180).toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String field, int maximumLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(field + " must have at most " + maximumLength + " characters");
        }
        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Set<FederativeUnit> getAssignedStates() {
        return Collections.unmodifiableSet(assignedStates);
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
