package br.com.sea.solicitations.user.adapter.out.security;

import br.com.sea.solicitations.user.application.port.out.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private static final int STRENGTH = 12;

    private final BCryptPasswordEncoder passwordEncoder;

    public BCryptPasswordHasher() {
        this.passwordEncoder = new BCryptPasswordEncoder(STRENGTH);
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(requireText(rawPassword, "Raw password"));
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(
                requireText(rawPassword, "Raw password"),
                requireText(passwordHash, "Password hash")
        );
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
