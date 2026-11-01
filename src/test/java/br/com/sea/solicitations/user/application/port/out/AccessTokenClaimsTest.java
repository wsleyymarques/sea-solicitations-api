package br.com.sea.solicitations.user.application.port.out;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.user.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessTokenClaimsTest {

    @Test
    void protectsCoveredStatesFromExternalChanges() {
        Set<FederativeUnit> states = new HashSet<>(Set.of(FederativeUnit.SP));

        AccessTokenClaims claims = new AccessTokenClaims(
                UUID.randomUUID(),
                UserRole.ANALYST,
                states
        );
        states.add(FederativeUnit.RJ);

        assertThat(claims.coveredStates()).containsExactly(FederativeUnit.SP);
        assertThatThrownBy(() -> claims.coveredStates().add(FederativeUnit.MG))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
