package br.edu.pucgoias.linkhealth;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Repete as regressões com as restrições reais do PostgreSQL. */
@SpringBootTest(properties = {
        "spring.datasource.url=${LINK_HEALTH_TEST_DB_URL}",
        "spring.datasource.username=${LINK_HEALTH_TEST_DB_USERNAME}",
        "spring.datasource.password=${LINK_HEALTH_TEST_DB_PASSWORD:}",
        "link-health.security.enabled=false",
        "link-health.photos.encryption-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
})
@ActiveProfiles("postgres")
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "LINK_HEALTH_TEST_DB_URL", matches = ".+")
class PostgresShoppingAndProductValidationTest extends ShoppingAndProductControllerTest { }
