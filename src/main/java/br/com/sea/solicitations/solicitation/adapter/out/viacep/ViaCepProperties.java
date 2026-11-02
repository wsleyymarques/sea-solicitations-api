package br.com.sea.solicitations.solicitation.adapter.out.viacep;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "integrations.viacep")
public record ViaCepProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {

    public ViaCepProperties {
        Objects.requireNonNull(baseUrl, "ViaCEP base URL is required");
        if (!baseUrl.isAbsolute()) {
            throw new IllegalArgumentException("ViaCEP base URL must be absolute");
        }
        connectTimeout = requirePositive(connectTimeout, "ViaCEP connect timeout");
        readTimeout = requirePositive(readTimeout, "ViaCEP read timeout");
    }

    private static Duration requirePositive(Duration duration, String field) {
        Objects.requireNonNull(duration, field + " is required");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return duration;
    }
}
