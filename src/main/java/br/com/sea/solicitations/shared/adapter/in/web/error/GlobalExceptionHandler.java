package br.com.sea.solicitations.shared.adapter.in.web.error;

import br.com.sea.solicitations.solicitation.application.exception.InvalidSolicitationStateException;
import br.com.sea.solicitations.solicitation.application.exception.SearchUnavailableException;
import br.com.sea.solicitations.solicitation.application.exception.PostalCodeNotFoundException;
import br.com.sea.solicitations.solicitation.application.exception.PostalCodeServiceUnavailableException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationAccessDeniedException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationConcurrentUpdateException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationNotFoundException;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationReviewAccessDeniedException;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;
import br.com.sea.solicitations.solicitation.domain.exception.IncompleteSolicitationException;
import br.com.sea.solicitations.user.application.exception.EmailAlreadyRegisteredException;
import br.com.sea.solicitations.user.application.exception.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldViolation> violations = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.FieldViolation(
                        error.getField(),
                        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()
                ))
                .sorted(Comparator
                        .comparing(ApiErrorResponse.FieldViolation::field)
                        .thenComparing(ApiErrorResponse.FieldViolation::message))
                .toList();

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                request,
                violations
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldViolation> violations = exception.getConstraintViolations()
                .stream()
                .map(violation -> new ApiErrorResponse.FieldViolation(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .sorted(Comparator
                        .comparing(ApiErrorResponse.FieldViolation::field)
                        .thenComparing(ApiErrorResponse.FieldViolation::message))
                .toList();

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                request,
                violations
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_JSON",
                "Request body is missing or malformed",
                request,
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message = exception.getRequiredType() == UUID.class
                ? "must be a valid UUID"
                : "has an invalid value";

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                request,
                List.of(new ApiErrorResponse.FieldViolation(exception.getName(), message))
        );
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_REGISTERED",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(SolicitationAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleSolicitationAccessDenied(
            SolicitationAccessDeniedException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.FORBIDDEN,
                "SOLICITATION_ACCESS_DENIED",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(SolicitationReviewAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleSolicitationReviewAccessDenied(
            SolicitationReviewAccessDeniedException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.FORBIDDEN,
                "SOLICITATION_REVIEW_ACCESS_DENIED",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(SolicitationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSolicitationNotFound(
            SolicitationNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "SOLICITATION_NOT_FOUND",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(IncompleteSolicitationException.class)
    public ResponseEntity<ApiErrorResponse> handleIncompleteSolicitation(
            IncompleteSolicitationException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldViolation> violations = exception.missingSteps()
                .stream()
                .map(step -> new ApiErrorResponse.FieldViolation(
                        submissionField(step),
                        "must be completed before submission"
                ))
                .toList();

        return response(
                HttpStatus.CONFLICT,
                "INCOMPLETE_SOLICITATION",
                exception.getMessage(),
                request,
                violations
        );
    }

    @ExceptionHandler(InvalidSolicitationStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSolicitationState(
            InvalidSolicitationStateException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                "INVALID_SOLICITATION_STATE",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(SolicitationConcurrentUpdateException.class)
    public ResponseEntity<ApiErrorResponse> handleSolicitationConcurrentUpdate(
            SolicitationConcurrentUpdateException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                "SOLICITATION_CONCURRENT_UPDATE",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(PostalCodeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePostalCodeNotFound(
            PostalCodeNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "POSTAL_CODE_NOT_FOUND",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(PostalCodeServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handlePostalCodeServiceUnavailable(
            PostalCodeServiceUnavailableException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "POSTAL_CODE_SERVICE_UNAVAILABLE",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(SearchUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleSearchUnavailable(
            SearchUnavailableException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SEARCH_UNAVAILABLE",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        LOGGER.error("Unexpected error while processing {}", request.getRequestURI(), exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                request,
                List.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<ApiErrorResponse.FieldViolation> violations
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                violations
        );
        return ResponseEntity.status(status).body(body);
    }

    private String submissionField(SolicitationStep step) {
        return switch (step) {
            case BASIC_DATA -> "basicData";
            case ADDRESS -> "address";
            case CONFIRMATION -> "confirmation";
            case COMPLETED -> throw new IllegalStateException(
                    "Completed cannot be a missing solicitation step"
            );
        };
    }
}
