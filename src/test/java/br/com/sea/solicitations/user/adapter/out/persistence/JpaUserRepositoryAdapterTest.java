package br.com.sea.solicitations.user.adapter.out.persistence;

import br.com.sea.solicitations.user.domain.User;
import br.com.sea.solicitations.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserRepositoryAdapterTest {

    @Mock
    private SpringDataUserJpaRepository userJpaRepository;

    private JpaUserRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaUserRepositoryAdapter(userJpaRepository);
    }

    @Test
    void checksEmailIgnoringCaseAndSurroundingWhitespace() {
        when(userJpaRepository.existsByEmailIgnoreCase("JOAO@EXAMPLE.COM")).thenReturn(true);

        boolean exists = adapter.existsByEmail("  JOAO@EXAMPLE.COM  ");

        assertThat(exists).isTrue();
        verify(userJpaRepository).existsByEmailIgnoreCase("JOAO@EXAMPLE.COM");
    }

    @Test
    void delegatesUserPersistenceToSpringData() {
        User user = new User("Joao Souza", "joao@example.com", "hashed-password", UserRole.CLIENT);
        when(userJpaRepository.save(user)).thenReturn(user);

        User savedUser = adapter.save(user);

        assertThat(savedUser).isSameAs(user);
        verify(userJpaRepository).save(user);
    }

    @Test
    void rejectsBlankEmailBeforeQueryingDatabase() {
        assertThatThrownBy(() -> adapter.existsByEmail("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is required");
    }
}
