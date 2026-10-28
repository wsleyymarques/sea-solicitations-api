package br.com.sea.solicitations.solicitation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class Decision {

    @Column(name = "decision_analyst_id")
    private UUID analystId;

    @Column(name = "decision_comment", length = 1000)
    private String comment;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected Decision() {
    }

    public Decision(UUID analystId, String comment, Instant decidedAt) {
        this.analystId = Objects.requireNonNull(analystId, "Analyst is required");
        this.comment = validateComment(comment);
        this.decidedAt = Objects.requireNonNull(decidedAt, "Decision time is required");
    }

    private static String validateComment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Decision comment is required");
        }
        String normalized = comment.trim();
        if (normalized.length() < 10 || normalized.length() > 1000) {
            throw new IllegalArgumentException("Decision comment must have between 10 and 1000 characters");
        }
        return normalized;
    }

    public UUID getAnalystId() {
        return analystId;
    }

    public String getComment() {
        return comment;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
