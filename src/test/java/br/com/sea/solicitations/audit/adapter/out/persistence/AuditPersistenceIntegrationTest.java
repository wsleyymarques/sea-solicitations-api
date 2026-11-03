package br.com.sea.solicitations.audit.adapter.out.persistence;

import br.com.sea.solicitations.user.application.exception.EmailAlreadyRegisteredException;
import br.com.sea.solicitations.user.application.port.in.RegisterClientCommand;
import br.com.sea.solicitations.user.application.port.in.RegisterClientResult;
import br.com.sea.solicitations.user.application.port.in.RegisterClientUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "outbox.polling.enabled=false"
})
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION", matches = "1")
class AuditPersistenceIntegrationTest {

    @Autowired private RegisterClientUseCase registration;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void successAndRolledBackFailureBothLeaveSeparateAuditRows() {
        String email = "audit-" + UUID.randomUUID() + "@example.com";
        RegisterClientCommand command = new RegisterClientCommand(
                "Audit Client", email, "private-password");

        RegisterClientResult created = registration.register(command);
        assertThatThrownBy(() -> registration.register(command))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?",
                Integer.class, email)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_logs
                WHERE action = 'USER_CREATED' AND outcome = 'SUCCESS' AND resource_id = ?
                """, Integer.class, created.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_logs
                WHERE action = 'USER_CREATED' AND outcome = 'FAILURE' AND resource_id IS NULL
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_logs WHERE metadata::text LIKE '%private-password%'
                """, Integer.class)).isZero();
    }
}
