package br.com.sea.solicitations.user.domain;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void assignsStatesOnlyToAnalysts() {
        User analyst = new User("Ana Silva", "ANA@EXAMPLE.COM", "encoded-password", UserRole.ANALYST);
        Set<FederativeUnit> states = new HashSet<>(Set.of(FederativeUnit.SP, FederativeUnit.RJ));

        analyst.assignStates(states);
        states.clear();

        assertThat(analyst.getEmail()).isEqualTo("ana@example.com");
        assertThat(analyst.getAssignedStates()).containsExactlyInAnyOrder(FederativeUnit.SP, FederativeUnit.RJ);
        assertThat(analyst.canReview(FederativeUnit.SP)).isTrue();
        assertThat(analyst.canReview(FederativeUnit.MG)).isFalse();
    }

    @Test
    void keepsAssignedStatesEmptyForClients() {
        User client = new User("Joao Souza", "joao@example.com", "encoded-password", UserRole.CLIENT);

        assertThat(client.getAssignedStates()).isEmpty();
        assertThat(client.canReview(FederativeUnit.SP)).isFalse();
        assertThatThrownBy(() -> client.assignStates(Set.of(FederativeUnit.SP)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only analysts can be assigned federative units");
    }
}
