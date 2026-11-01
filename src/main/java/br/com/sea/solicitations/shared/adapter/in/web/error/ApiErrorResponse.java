package br.com.sea.solicitations.shared.adapter.in.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<FieldViolation> violations
) {

    public ApiErrorResponse {
        Objects.requireNonNull(timestamp, "Timestamp is required");
        Objects.requireNonNull(error, "Error is required");
        Objects.requireNonNull(code, "Code is required");
        Objects.requireNonNull(message, "Message is required");
        Objects.requireNonNull(path, "Path is required");
        violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public record FieldViolation(String field, String message) {

        public FieldViolation {
            Objects.requireNonNull(field, "Field is required");
            Objects.requireNonNull(message, "Violation message is required");
        }
    }
}
