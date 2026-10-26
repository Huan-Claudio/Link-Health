package br.edu.pucgoias.linkhealth;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        properties = {
            "spring.datasource.url=${LINK_HEALTH_TEST_DB_URL}",
            "spring.datasource.username=${LINK_HEALTH_TEST_DB_USERNAME}",
            "spring.datasource.password=${LINK_HEALTH_TEST_DB_PASSWORD:}",
            "link-health.security.api-key=link-health-test-api-key-12345678901234567890",
            "link-health.security.rate-limit-per-minute=10000",
            "link-health.photos.encryption-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
        })
@ActiveProfiles("postgres")
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "LINK_HEALTH_TEST_DB_URL", matches = ".+")
class PostgresRecordsMobileTest extends RecordsMobileContract {}
