package br.com.sea.solicitations.shared.adapter.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void createsValidPropertiesAndNormalizesText() {
        JwtProperties properties = new JwtProperties(
                "  sea-solicitations-api  ",
                Duration.ofHours(1),
                "  base64-secret  "
        );

        assertThat(properties.issuer()).isEqualTo("sea-solicitations-api");
        assertThat(properties.accessTokenTtl()).isEqualTo(Duration.ofHours(1));
        assertThat(properties.secret()).isEqualTo("base64-secret");
    }

    @Test
    void rejectsBlankIssuer() {
        assertThatThrownBy(() -> new JwtProperties(" ", Duration.ofHours(1), "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT issuer is required");
    }

    @Test
    void rejectsMissingAccessTokenTtl() {
        assertThatThrownBy(() -> new JwtProperties("issuer", null, "secret"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("JWT access token TTL is required");
    }

    @Test
    void rejectsNonPositiveAccessTokenTtl() {
        assertThatThrownBy(() -> new JwtProperties("issuer", Duration.ZERO, "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT access token TTL must be greater than zero");

        assertThatThrownBy(() -> new JwtProperties("issuer", Duration.ofMinutes(-1), "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT access token TTL must be greater than zero");
    }

    @Test
    void rejectsBlankSecret() {
        assertThatThrownBy(() -> new JwtProperties("issuer", Duration.ofHours(1), " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT secret is required");
    }

    @Test
    void redactsSecretFromStringRepresentation() {
        JwtProperties properties = new JwtProperties(
                "sea-solicitations-api",
                Duration.ofHours(1),
                "sensitive-secret"
        );

        assertThat(properties.toString())
                .doesNotContain("sensitive-secret")
                .contains("secret=[REDACTED]");
    }
}
