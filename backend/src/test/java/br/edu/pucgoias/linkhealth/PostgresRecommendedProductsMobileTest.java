package br.edu.pucgoias.linkhealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Executa o fluxo móvel no banco de testes configurado, revertendo os dados ao terminar. */
@SpringBootTest(properties = {
        "spring.datasource.url=${LINK_HEALTH_TEST_DB_URL}",
        "spring.datasource.username=${LINK_HEALTH_TEST_DB_USERNAME}",
        "spring.datasource.password=${LINK_HEALTH_TEST_DB_PASSWORD:}",
        "link-health.security.api-key=link-health-test-api-key-12345678901234567890",
        "link-health.photos.encryption-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
})
@ActiveProfiles("postgres")
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "LINK_HEALTH_TEST_DB_URL", matches = ".+")
class PostgresRecommendedProductsMobileTest extends RecommendedProductsMobileTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void migrationInstallsThePatientForeignKey() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM pg_constraint WHERE conname = ? AND contype = 'f'",
                Integer.class, "fk_recommended_products_patient");
        assertEquals(1, count);
    }
}
