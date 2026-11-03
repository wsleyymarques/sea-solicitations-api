package br.com.sea.solicitations.user.adapter.out.persistence;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.user.adapter.out.security.BCryptPasswordHasher;
import br.com.sea.solicitations.user.application.exception.EmailAlreadyRegisteredException;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserCommand;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserResult;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserUseCase;
import br.com.sea.solicitations.user.application.port.out.PasswordHasher;
import br.com.sea.solicitations.user.application.port.out.UserRepository;
import br.com.sea.solicitations.user.application.service.CreateInternalUserService;
import br.com.sea.solicitations.user.domain.User;
import br.com.sea.solicitations.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaUserRepositoryAdapter.class,
        BCryptPasswordHasher.class,
        InternalUserPersistenceIntegrationTest.LocalBeans.class
})
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION", matches = "1")
class InternalUserPersistenceIntegrationTest {

    @Autowired
    private CreateInternalUserUseCase useCase;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private JpaUserRepositoryAdapter userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void persistsAnalystWithCoverageAndBcryptPassword() {
        CreateInternalUserResult result = useCase.create(new CreateInternalUserCommand(
                "  Ana Silva  ",
                "ANA@EXAMPLE.COM",
                "StrongPassword@123",
                UserRole.ANALYST,
                Set.of(FederativeUnit.SP, FederativeUnit.RJ)
        ));
        entityManager.flush();

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?",
                String.class,
                result.id()
        );
        List<String> coveredStates = jdbcTemplate.queryForList(
                "SELECT state FROM analyst_coverages WHERE analyst_id = ? ORDER BY state",
                String.class,
                result.id()
        );

        assertThat(result.name()).isEqualTo("Ana Silva");
        assertThat(result.email()).isEqualTo("ana@example.com");
        assertThat(result.coveredStates()).containsExactlyInAnyOrder(FederativeUnit.SP, FederativeUnit.RJ);
        assertThat(passwordHash).startsWith("$2");
        assertThat(passwordHash).doesNotContain("StrongPassword@123");
        assertThat(passwordHasher.matches("StrongPassword@123", passwordHash)).isTrue();
        assertThat(coveredStates).containsExactly("RJ", "SP");
    }

    @Test
    void persistsAdministratorWithoutAnalystCoverage() {
        CreateInternalUserResult result = useCase.create(new CreateInternalUserCommand(
                "Admin Two",
                "admin.two@example.com",
                "StrongPassword@123",
                UserRole.ADMIN,
                Set.of()
        ));
        entityManager.flush();

        Integer coverageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analyst_coverages WHERE analyst_id = ?",
                Integer.class,
                result.id()
        );

        assertThat(result.role()).isEqualTo(UserRole.ADMIN);
        assertThat(result.coveredStates()).isEmpty();
        assertThat(coverageCount).isZero();
    }

    @Test
    void mapsDatabaseDuplicateDetectedAfterPrecheckToDomainConflict() {
        CreateInternalUserCommand command = new CreateInternalUserCommand(
                "Admin Existing",
                "race@example.com",
                "StrongPassword@123",
                UserRole.ADMIN,
                Set.of()
        );
        useCase.create(command);

        UserRepository stalePrecheckRepository = new UserRepository() {
            @Override
            public boolean existsByEmail(String email) {
                return false;
            }

            @Override
            public Optional<User> findByEmail(String email) {
                return userRepository.findByEmail(email);
            }

            @Override
            public User save(User user) {
                return userRepository.save(user);
            }
        };
        CreateInternalUserUseCase competingUseCase = new CreateInternalUserService(
                stalePrecheckRepository,
                passwordHasher
        );

        assertThatThrownBy(() -> competingUseCase.create(command))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email is already registered");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LocalBeans {

        @Bean
        @Primary
        CreateInternalUserUseCase createInternalUserUseCase(
                JpaUserRepositoryAdapter userRepository,
                PasswordHasher passwordHasher
        ) {
            return new CreateInternalUserService(userRepository, passwordHasher);
        }
    }
}
