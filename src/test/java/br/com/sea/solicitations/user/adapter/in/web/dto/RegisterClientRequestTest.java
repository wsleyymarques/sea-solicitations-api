package br.com.sea.solicitations.user.adapter.in.web.dto;

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

class RegisterClientRequestTest {

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
    void acceptsValidRequest() {
        RegisterClientRequest request = new RegisterClientRequest(
                "Joao Souza",
                "joao@example.com",
                "StrongPassword@123"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsInvalidFields() {
        RegisterClientRequest request = new RegisterClientRequest("  ", "invalid-email", "short");

        Set<String> invalidFields = validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidFields).containsExactlyInAnyOrder("name", "email", "password");
    }

    @Test
    void doesNotExposePasswordInLogsOrJson() throws JsonProcessingException {
        RegisterClientRequest request = new RegisterClientRequest(
                "Joao Souza",
                "joao@example.com",
                "StrongPassword@123"
        );

        String json = new ObjectMapper().writeValueAsString(request);

        assertThat(request.toString())
                .doesNotContain("StrongPassword@123")
                .contains("password=[REDACTED]");
        assertThat(json)
                .doesNotContain("StrongPassword@123")
                .doesNotContain("password");
    }
}
