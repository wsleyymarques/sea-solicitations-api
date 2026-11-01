package br.com.sea.solicitations.user.adapter.out.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher passwordHasher = new BCryptPasswordHasher();

    @Test
    void verifiesRawPasswordAgainstStoredHash() {
        String passwordHash = passwordHasher.hash("correct-password");

        assertThat(passwordHasher.matches("correct-password", passwordHash)).isTrue();
        assertThat(passwordHasher.matches("wrong-password", passwordHash)).isFalse();
    }
}
