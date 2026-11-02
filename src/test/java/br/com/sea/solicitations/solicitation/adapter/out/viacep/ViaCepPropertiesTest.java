package br.com.sea.solicitations.solicitation.adapter.out.viacep;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ViaCepPropertiesTest {

    @Test
    void acceptsAbsoluteUrlAndPositiveTimeouts() {
        ViaCepProperties properties = new ViaCepProperties(
                URI.create("https://viacep.com.br"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(3)
        );

        assertThat(properties.baseUrl()).isEqualTo(URI.create("https://viacep.com.br"));
        assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void rejectsRelativeBaseUrl() {
        assertThatThrownBy(() -> new ViaCepProperties(
                URI.create("viacep"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(3)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ViaCEP base URL must be absolute");
    }

    @Test
    void rejectsNonPositiveConnectTimeout() {
        assertThatThrownBy(() -> new ViaCepProperties(
                URI.create("https://viacep.com.br"),
                Duration.ZERO,
                Duration.ofSeconds(3)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ViaCEP connect timeout must be greater than zero");
    }

    @Test
    void rejectsNonPositiveReadTimeout() {
        assertThatThrownBy(() -> new ViaCepProperties(
                URI.create("https://viacep.com.br"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(-1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ViaCEP read timeout must be greater than zero");
    }
}
