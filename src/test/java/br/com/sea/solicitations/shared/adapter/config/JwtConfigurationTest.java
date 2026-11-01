package br.com.sea.solicitations.shared.adapter.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigurationTest {

    private final JwtConfiguration configuration = new JwtConfiguration();

    @Test
    void createsHs256KeyFromBase64Secret() {
        String secret = Base64.getEncoder().encodeToString(new byte[32]);
        JwtProperties properties = new JwtProperties("issuer", Duration.ofHours(1), secret);

        assertThat(configuration.jwtSecretKey(properties))
                .extracting(key -> key.getEncoded().length, key -> key.getAlgorithm())
                .containsExactly(32, "HmacSHA256");
    }

    @Test
    void rejectsMalformedBase64Secret() {
        JwtProperties properties = new JwtProperties("issuer", Duration.ofHours(1), "not-base64!");

        assertThatThrownBy(() -> configuration.jwtSecretKey(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT secret must be valid Base64");
    }

    @Test
    void rejectsSecretShorterThan256Bits() {
        String secret = Base64.getEncoder().encodeToString(new byte[31]);
        JwtProperties properties = new JwtProperties("issuer", Duration.ofHours(1), secret);

        assertThatThrownBy(() -> configuration.jwtSecretKey(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT secret must decode to at least 32 bytes");
    }
}
