package br.edu.pucgoias.linkhealth;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
            "link-health.security.api-key=link-health-test-api-key-12345678901234567890",
            "link-health.security.rate-limit-per-minute=10000",
            "link-health.photos.encryption-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
        })
@AutoConfigureMockMvc
class RecordsMobileTest extends RecordsMobileContract {}
