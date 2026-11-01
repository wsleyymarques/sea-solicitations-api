package br.com.sea.solicitations.user.application.port.out;

/**
 * Output port used by the application to issue access tokens.
 */
@FunctionalInterface
public interface AccessTokenGenerator {

    GeneratedAccessToken generate(AccessTokenClaims claims);
}
