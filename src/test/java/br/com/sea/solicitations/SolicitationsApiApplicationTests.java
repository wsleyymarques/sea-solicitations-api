package br.com.sea.solicitations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.autoconfigure.exclude="
                    + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
        }
)
class SolicitationsApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
