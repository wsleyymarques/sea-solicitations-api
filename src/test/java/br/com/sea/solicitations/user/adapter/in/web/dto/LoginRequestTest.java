package br.com.sea.solicitations.user.adapter.in.web.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validatesEmailAndPassword() {
        LoginRequest request = new LoginRequest("invalid-email", "");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("email", "password");
    }

    @Test
    void neverSerializesPassword() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "sensitive-password");

        assertThat(request.toString()).doesNotContain("sensitive-password");
        assertThat(objectMapper.writeValueAsString(request)).doesNotContain("sensitive-password");
    }
}
