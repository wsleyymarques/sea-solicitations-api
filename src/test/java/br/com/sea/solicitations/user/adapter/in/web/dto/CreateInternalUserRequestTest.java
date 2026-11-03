package br.com.sea.solicitations.user.adapter.in.web.dto;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.user.domain.UserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInternalUserRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsValidAnalystRequest() {
        CreateInternalUserRequest request = new CreateInternalUserRequest(
                "Ana Silva",
                "ana@example.com",
                "StrongPassword@123",
                UserRole.ANALYST,
                Set.of(FederativeUnit.SP)
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsInvalidFields() {
        CreateInternalUserRequest request = new CreateInternalUserRequest(
                "  ",
                "invalid-email",
                "short",
                null,
                Set.of()
        );

        Set<String> invalidFields = validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidFields).containsExactlyInAnyOrder("name", "email", "password", "role");
    }

    @Test
    void doesNotExposePasswordInLogsOrJson() throws JsonProcessingException {
        CreateInternalUserRequest request = new CreateInternalUserRequest(
                "Admin Two",
                "admin.two@example.com",
                "StrongPassword@123",
                UserRole.ADMIN,
                Set.of()
        );

        String json = new ObjectMapper().writeValueAsString(request);

        assertThat(request.toString())
                .doesNotContain("StrongPassword@123")
                .contains("password=[REDACTED]");
        assertThat(json)
                .doesNotContain("StrongPassword@123")
                .doesNotContain("password");
    }

    @Test
    void enforcesTextLengthBoundaries() {
        CreateInternalUserRequest minimumPassword = new CreateInternalUserRequest(
                "a".repeat(120),
                "admin@example.com",
                "x".repeat(8),
                UserRole.ADMIN,
                Set.of()
        );
        CreateInternalUserRequest maximumPassword = new CreateInternalUserRequest(
                "Admin",
                "admin@example.com",
                "x".repeat(72),
                UserRole.ADMIN,
                Set.of()
        );
        CreateInternalUserRequest beyondLimits = new CreateInternalUserRequest(
                "a".repeat(121),
                "admin@example.com",
                "x".repeat(73),
                UserRole.ADMIN,
                Set.of()
        );

        assertThat(validator.validate(minimumPassword)).isEmpty();
        assertThat(validator.validate(maximumPassword)).isEmpty();
        assertThat(validator.validate(beyondLimits))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("name", "password");
    }
}
