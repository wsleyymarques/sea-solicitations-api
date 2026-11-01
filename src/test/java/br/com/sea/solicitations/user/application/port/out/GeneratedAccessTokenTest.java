package br.com.sea.solicitations.user.application.port.out;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratedAccessTokenTest {

    @Test
    void redactsTokenFromStringRepresentation() {
        GeneratedAccessToken token = new GeneratedAccessToken("signed.jwt.value", 3600L);

        assertThat(token.toString())
                .doesNotContain("signed.jwt.value")
                .contains("value=REDACTED")
                .contains("expiresIn=3600");
    }

    @Test
    void rejectsNonPositiveExpirationTime() {
        assertThatThrownBy(() -> new GeneratedAccessToken("signed.jwt.value", 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expiration time must be greater than zero");
    }
}
