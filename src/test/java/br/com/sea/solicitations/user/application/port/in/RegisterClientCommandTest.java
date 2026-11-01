package br.com.sea.solicitations.user.application.port.in;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterClientCommandTest {

    @Test
    void redactsRawPasswordFromStringRepresentation() {
        RegisterClientCommand command = new RegisterClientCommand(
                "Joao Souza",
                "joao@example.com",
                "my-secret-password"
        );

        assertThat(command.toString())
                .doesNotContain("my-secret-password")
                .contains("rawPassword=[REDACTED]");
    }
}
