package br.edu.pucgoias.linkhealth;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(named = "LINK_HEALTH_TEST_DB_URL", matches = ".+")
@TestPropertySource(properties = {
    "spring.datasource.url=${LINK_HEALTH_TEST_DB_URL}",
    "spring.datasource.username=${LINK_HEALTH_TEST_DB_USERNAME}",
    "spring.datasource.password=${LINK_HEALTH_TEST_DB_PASSWORD:}"
})
class PostgresClinicalMobileTest extends ClinicalMobileContract {}
