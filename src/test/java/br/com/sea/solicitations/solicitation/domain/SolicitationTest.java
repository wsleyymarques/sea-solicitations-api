package br.com.sea.solicitations.solicitation.domain;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.domain.exception.IncompleteSolicitationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolicitationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);
    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");

    @Test
    void completesAllStagesAndSubmitsTheSolicitation() {
        UUID clientId = UUID.randomUUID();
        Solicitation solicitation = new Solicitation(clientId);

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(solicitation.getCurrentStep()).isEqualTo(SolicitationStep.BASIC_DATA);

        solicitation.completeBasicData(validBasicData());
        assertThat(solicitation.getCurrentStep()).isEqualTo(SolicitationStep.ADDRESS);

        solicitation.completeAddress(validAddress());
        assertThat(solicitation.getCurrentStep()).isEqualTo(SolicitationStep.CONFIRMATION);

        solicitation.completeConfirmation(validConfirmation(), TODAY);
        assertThat(solicitation.getCurrentStep()).isEqualTo(SolicitationStep.COMPLETED);

        solicitation.submit(TODAY, NOW);

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.SUBMITTED);
        assertThat(solicitation.getSubmittedAt()).isEqualTo(NOW);
        assertThat(solicitation.isOwnedBy(clientId)).isTrue();
    }

    @Test
    void requiresStagesToBeCompletedInOrder() {
        Solicitation solicitation = new Solicitation(UUID.randomUUID());

        assertThatThrownBy(() -> solicitation.completeAddress(validAddress()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Basic data must be completed before the address");
    }

    @Test
    void allowsOnlyTheAssignedAnalystToDecide() {
        Solicitation solicitation = submittedSolicitation();
        UUID assignedAnalyst = UUID.randomUUID();
        UUID anotherAnalyst = UUID.randomUUID();

        solicitation.startReview(assignedAnalyst, NOW.plusSeconds(60));

        assertThatThrownBy(() -> solicitation.approve(
                anotherAnalyst,
                "Solicitacao aprovada com sucesso",
                NOW.plusSeconds(120)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only the assigned analyst can decide this solicitation");

        solicitation.approve(
                assignedAnalyst,
                "Solicitacao aprovada com sucesso",
                NOW.plusSeconds(120)
        );

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.APPROVED);
        assertThat(solicitation.getDecision().getAnalystId()).isEqualTo(assignedAnalyst);
    }

    @Test
    void preventsEditingAfterSubmission() {
        Solicitation solicitation = submittedSolicitation();

        assertThatThrownBy(() -> solicitation.completeBasicData(validBasicData()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Solicitation must be DRAFT but is SUBMITTED");
    }

    @Test
    void requiresMinimumEstimatedValueForHighPriority() {
        assertThatThrownBy(() -> new Confirmation(
                Priority.HIGH,
                TODAY.plusDays(1),
                new BigDecimal("99.99"),
                true
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("High priority requires an estimated value of at least 100.00");
    }

    @Test
    void reportsEveryMissingStageInOrderWhenSubmissionIsIncomplete() {
        Solicitation solicitation = new Solicitation(UUID.randomUUID());

        assertThatExceptionOfType(IncompleteSolicitationException.class)
                .isThrownBy(() -> solicitation.submit(TODAY, NOW))
                .satisfies(exception -> assertThat(exception.missingSteps()).containsExactly(
                        SolicitationStep.BASIC_DATA,
                        SolicitationStep.ADDRESS,
                        SolicitationStep.CONFIRMATION
                ));
        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(solicitation.getSubmittedAt()).isNull();
    }

    @Test
    void doesNotMutateSolicitationWhenPreferredDateExpiredBeforeSubmission() {
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        solicitation.completeBasicData(validBasicData());
        solicitation.completeAddress(validAddress());
        solicitation.completeConfirmation(new Confirmation(
                Priority.MEDIUM,
                TODAY.minusDays(1),
                null,
                true
        ), TODAY.minusDays(2));

        assertThatThrownBy(() -> solicitation.submit(TODAY, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Preferred date cannot be in the past");

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(solicitation.getSubmittedAt()).isNull();
    }

    private static Solicitation submittedSolicitation() {
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        solicitation.completeBasicData(validBasicData());
        solicitation.completeAddress(validAddress());
        solicitation.completeConfirmation(validConfirmation(), TODAY);
        solicitation.submit(TODAY, NOW);
        return solicitation;
    }

    private static BasicData validBasicData() {
        return new BasicData(
                ServiceType.INSTALLATION,
                "Instalacao de equipamento",
                "Solicito a instalacao de um novo equipamento no endereco informado."
        );
    }

    private static Address validAddress() {
        return new Address(
                "01001-000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                FederativeUnit.SP,
                "100",
                null
        );
    }

    private static Confirmation validConfirmation() {
        return new Confirmation(
                Priority.MEDIUM,
                TODAY.plusDays(1),
                new BigDecimal("50.00"),
                true
        );
    }
}
