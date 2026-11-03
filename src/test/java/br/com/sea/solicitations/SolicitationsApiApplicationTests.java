package br.com.sea.solicitations;

import br.com.sea.solicitations.audit.adapter.out.persistence.SpringDataAuditLogRepository;
import br.com.sea.solicitations.outbox.adapter.out.persistence.SpringDataOutboxEventJpaRepository;
import br.com.sea.solicitations.solicitation.adapter.out.persistence.SpringDataSolicitationJpaRepository;
import br.com.sea.solicitations.user.adapter.out.persistence.SpringDataUserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
            "security.jwt.issuer=sea-solicitations-api",
            "security.jwt.access-token-ttl=1h",
            "security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "outbox.polling.enabled=false",
            "spring.autoconfigure.exclude="
                    + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
        }
)
class SolicitationsApiApplicationTests {

    @MockitoBean
    private SpringDataUserJpaRepository userJpaRepository;

    @MockitoBean
    private SpringDataSolicitationJpaRepository solicitationJpaRepository;

    @MockitoBean
    private SpringDataOutboxEventJpaRepository outboxEventJpaRepository;

    @MockitoBean
    private SpringDataAuditLogRepository auditLogRepository;

    @Test
    void contextLoads() {
    }
}
