package br.com.sea.solicitations.shared.adapter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "security.jwt")
public final class JwtProperties {

    private final String issuer;
    private final Duration accessTokenTtl;
    private final String secret;

    public JwtProperties(
            String issuer,
            Duration accessTokenTtl,
            String secret
    ) {
        this.issuer = requireText(issuer, "JWT issuer");
        this.accessTokenTtl = requirePositiveDuration(accessTokenTtl);
        this.secret = requireText(secret, "JWT secret");
    }


    public String issuer() {
        return issuer;
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }

    public String secret() {
        return secret;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static Duration requirePositiveDuration(Duration duration) {
        Objects.requireNonNull(duration, "JWT access token TTL is required");

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("JWT access token TTL must be greater than zero");
        }
        return duration;
    }

    @Override
    public String toString() {
        return "JwtProperties[" +
                "issuer=" + issuer +
                ", accessTokenTtl=" + accessTokenTtl +
                ", secret=[REDACTED]" +
                ']';
    }
}
