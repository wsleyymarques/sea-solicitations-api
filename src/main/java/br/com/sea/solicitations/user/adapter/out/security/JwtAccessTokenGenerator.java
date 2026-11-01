package br.com.sea.solicitations.user.adapter.out.security;

import br.com.sea.solicitations.shared.adapter.config.JwtProperties;
import br.com.sea.solicitations.user.application.port.out.AccessTokenClaims;
import br.com.sea.solicitations.user.application.port.out.AccessTokenGenerator;
import br.com.sea.solicitations.user.application.port.out.GeneratedAccessToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Component
public class JwtAccessTokenGenerator implements AccessTokenGenerator {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtAccessTokenGenerator(
            JwtEncoder jwtEncoder,
            JwtProperties properties,
            Clock clock
    ) {
        this.jwtEncoder = Objects.requireNonNull(jwtEncoder, "JWT encoder is required");
        this.properties = Objects.requireNonNull(properties, "JWT properties are required");
        this.clock = Objects.requireNonNull(clock, "Clock is required");
    }

    @Override
    public GeneratedAccessToken generate(AccessTokenClaims claims) {
        Objects.requireNonNull(claims, "Access token claims are required");

        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        List<String> coveredStates = claims.coveredStates()
                .stream()
                .map(Enum::name)
                .sorted()
                .toList();

        JwtClaimsSet claimSet = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(claims.userId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", claims.role().name())
                .claim("coveredStates", coveredStates)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        String tokenValue = jwtEncoder
                .encode(JwtEncoderParameters.from(header, claimSet))
                .getTokenValue();

        return new GeneratedAccessToken(
                tokenValue,
                properties.accessTokenTtl().toSeconds()
        );
    }
}
