package br.com.sea.solicitations.solicitation.adapter.out.persistence;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.SolicitationConcurrentUpdateException;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationPage;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaSolicitationRepositoryAdapter.class)
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION", matches = "1")
class SpringDataSolicitationJpaRepositoryIntegrationTest {

    private static final UUID CLIENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ANALYST_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ANALYST_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final Instant NOW = Instant.parse("2026-11-02T12:00:00Z");

    @Autowired
    private SolicitationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void filtersByCoverageStatusAndAssignmentWithStablePagination() {
        insertUser(CLIENT_ID, "client@example.com", "CLIENT");
        insertUser(ANALYST_ID, "analyst@example.com", "ANALYST");
        insertUser(OTHER_ANALYST_ID, "other-analyst@example.com", "ANALYST");

        UUID firstSubmitted = UUID.fromString("30000000-0000-0000-0000-000000000001");
        UUID secondSubmitted = UUID.fromString("30000000-0000-0000-0000-000000000002");
        UUID ownReview = UUID.fromString("30000000-0000-0000-0000-000000000003");
        insertSolicitation(firstSubmitted, "SP", "SUBMITTED", null, NOW, null);
        insertSolicitation(secondSubmitted, "SP", "SUBMITTED", null, NOW, null);
        insertSolicitation(
                ownReview,
                "SP",
                "IN_REVIEW",
                ANALYST_ID,
                NOW.minusSeconds(60),
                NOW.minusSeconds(30)
        );

        insertSolicitation(
                UUID.fromString("30000000-0000-0000-0000-000000000004"),
                "SP",
                "IN_REVIEW",
                OTHER_ANALYST_ID,
                NOW.plusSeconds(60),
                NOW.plusSeconds(90)
        );
        insertSolicitation(
                UUID.fromString("30000000-0000-0000-0000-000000000005"),
                "RJ",
                "SUBMITTED",
                null,
                NOW.plusSeconds(120),
                null
        );
        insertSolicitation(
                UUID.fromString("30000000-0000-0000-0000-000000000006"),
                "SP",
                "DRAFT",
                null,
                null,
                null
        );

        SolicitationPage firstPage = repository.findVisibleToAnalyst(
                ANALYST_ID, Set.of(FederativeUnit.SP), 0, 2
        );
        SolicitationPage secondPage = repository.findVisibleToAnalyst(
                ANALYST_ID, Set.of(FederativeUnit.SP), 1, 2
        );

        assertThat(firstPage.content())
                .extracting(solicitation -> solicitation.getId())
                .containsExactly(secondSubmitted, firstSubmitted);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content())
                .extracting(solicitation -> solicitation.getId())
                .containsExactly(ownReview);
        assertThat(secondPage.totalElements()).isEqualTo(3);
        assertThat(secondPage.totalPages()).isEqualTo(2);
    }

    @Test
    void translatesARealPostgresStaleVersionWriteToTheApplicationConflict() {
        insertUser(CLIENT_ID, "stale-client@example.com", "CLIENT");
        insertUser(ANALYST_ID, "stale-analyst@example.com", "ANALYST");
        insertUser(OTHER_ANALYST_ID, "stale-other-analyst@example.com", "ANALYST");
        UUID solicitationId = UUID.fromString("30000000-0000-0000-0000-000000000010");
        insertSolicitation(solicitationId, "SP", "SUBMITTED", null, NOW, null);

        Solicitation firstWriter = repository.findById(solicitationId).orElseThrow();
        entityManager.detach(firstWriter);
        Solicitation staleWriter = repository.findById(solicitationId).orElseThrow();
        entityManager.detach(staleWriter);

        firstWriter.startReview(ANALYST_ID, NOW.plusSeconds(1));
        repository.save(firstWriter);

        staleWriter.startReview(OTHER_ANALYST_ID, NOW.plusSeconds(2));
        assertThatThrownBy(() -> repository.save(staleWriter))
                .isInstanceOf(SolicitationConcurrentUpdateException.class)
                .hasMessage("Solicitation was updated by another request");
    }

    private void insertUser(UUID id, String email, String role) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    id, full_name, email, password_hash, role, active,
                    created_at, updated_at, version
                ) VALUES (?, ?, ?, ?, ?, TRUE, ?, ?, 0)
                """,
                id,
                "Integration Test User",
                email,
                "not-used",
                role,
                Timestamp.from(NOW),
                Timestamp.from(NOW)
        );
    }

    private void insertSolicitation(
            UUID id,
            String state,
            String status,
            UUID assignedAnalystId,
            Instant submittedAt,
            Instant reviewStartedAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO solicitations (
                    id, client_id, service_type, title, description,
                    postal_code, street, neighborhood, city, state, address_number,
                    priority, preferred_date, terms_accepted,
                    status, current_step, assigned_analyst_id,
                    submitted_at, review_started_at,
                    created_at, updated_at, version
                ) VALUES (
                    ?, ?, 'INSTALLATION', ?, ?,
                    '01001000', 'Praca da Se', 'Se', 'Sao Paulo', ?, '100',
                    'MEDIUM', ?, TRUE,
                    ?, 'COMPLETED', ?,
                    ?, ?,
                    ?, ?, 0
                )
                """,
                id,
                CLIENT_ID,
                "Solicitation " + id,
                "Descricao completa para o teste de integracao da consulta.",
                state,
                LocalDate.of(2099, 1, 1),
                status,
                assignedAnalystId,
                submittedAt == null ? null : Timestamp.from(submittedAt),
                reviewStartedAt == null ? null : Timestamp.from(reviewStartedAt),
                Timestamp.from(NOW),
                Timestamp.from(NOW)
        );
    }
}
