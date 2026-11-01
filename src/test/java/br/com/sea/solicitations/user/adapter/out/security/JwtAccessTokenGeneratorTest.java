package br.com.sea.solicitations.user.adapter.out.security;

import br.com.sea.solicitations.shared.adapter.config.JwtConfiguration;
import br.com.sea.solicitations.shared.adapter.config.JwtProperties;
import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.user.application.port.out.AccessTokenClaims;
import br.com.sea.solicitations.user.application.port.out.GeneratedAccessToken;
import br.com.sea.solicitations.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAccessTokenGeneratorTest {

    @Test
    void generatesSignedTokenWithIdentityAndAuthorizationClaims() {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Clock clock = Clock.fixed(issuedAt, ZoneOffset.UTC);
        JwtProperties properties = new JwtProperties(
                "sea-solicitations-api",
                Duration.ofHours(1),
                Base64.getEncoder().encodeToString(new byte[32])
        );
        JwtConfiguration configuration = new JwtConfiguration();
        var secretKey = configuration.jwtSecretKey(properties);
        JwtEncoder encoder = configuration.jwtEncoder(secretKey);
        JwtDecoder decoder = configuration.jwtDecoder(secretKey, properties);
        JwtAccessTokenGenerator generator = new JwtAccessTokenGenerator(encoder, properties, clock);
        UUID userId = UUID.randomUUID();

        GeneratedAccessToken generatedToken = generator.generate(new AccessTokenClaims(
                userId,
                UserRole.ANALYST,
                Set.of(FederativeUnit.SP, FederativeUnit.RJ)
        ));
        Jwt decodedToken = decoder.decode(generatedToken.value());

        assertThat(generatedToken.expiresIn()).isEqualTo(3600L);
        assertThat(decodedToken.getSubject()).isEqualTo(userId.toString());
        assertThat(decodedToken.getClaimAsString("iss")).isEqualTo("sea-solicitations-api");
        assertThat(decodedToken.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(decodedToken.getExpiresAt()).isEqualTo(issuedAt.plus(Duration.ofHours(1)));
        assertThat(decodedToken.getClaimAsString("role")).isEqualTo("ANALYST");
        assertThat(decodedToken.getClaimAsStringList("coveredStates"))
                .containsExactly("RJ", "SP");
    }
}
