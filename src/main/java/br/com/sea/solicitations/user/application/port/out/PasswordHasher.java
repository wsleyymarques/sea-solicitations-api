package br.com.sea.solicitations.user.application.port.out;

/**
 * Output port used by the application to protect and verify raw passwords.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
