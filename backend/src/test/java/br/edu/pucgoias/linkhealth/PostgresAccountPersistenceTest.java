package br.edu.pucgoias.linkhealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.UserAccount;
import br.edu.pucgoias.linkhealth.domain.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=${LINK_HEALTH_TEST_DB_URL}",
        "spring.datasource.username=${LINK_HEALTH_TEST_DB_USERNAME}",
        "spring.datasource.password=${LINK_HEALTH_TEST_DB_PASSWORD:}",
        "link-health.security.api-key=link-health-test-api-key-12345678901234567890",
        "link-health.photos.encryption-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
})
@ActiveProfiles("postgres")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "LINK_HEALTH_TEST_DB_URL", matches = ".+")
class PostgresAccountPersistenceTest {
    private static final String API_KEY = "link-health-test-api-key-12345678901234567890";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void persistsProfilesAndRollsBackTheAccountIfItsProfileCannotBeSaved() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String document = suffix.toUpperCase(Locale.ROOT);
        String patientEmail = "maria-" + suffix + "@exemplo.com";
        UUID patientId = register("Maria Silva", patientEmail, document, "PACIENTE");
        UUID nutritionistId = register("Ana", "ana-" + suffix + "@exemplo.com", "CRN-" + document,
                "NUTRICIONISTA");

        assertEquals(1, count("user_accounts", "id", patientId));
        assertEquals(1, count("patients", "user_id", patientId));
        assertEquals(0, count("nutritionists", "user_id", patientId));
        assertEquals(1, count("nutritionists", "user_id", nutritionistId));
        UserAccount reloaded = repository.findByEmail(patientEmail).orElseThrow();
        assertEquals(patientId, reloaded.id());
        assertEquals(document, repository.findById(patientId).orElseThrow().document());
        assertEquals(nutritionistId, repository.findByDocument("CRN-" + document).orElseThrow().id());

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", patientEmail, "password", "senha1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mockMvc.perform(get("/api/v1/patients/search").param("query", patientEmail)
                        .header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(patientId.toString()));

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into patients (user_id, cpf) values (?, ?)", nutritionistId, "other-" + suffix));
        String duplicateEmail = "duplicate-" + suffix + "@exemplo.com";
        assertThrows(InvalidRequestException.class, () -> repository.save(new UserAccount(
                UUID.randomUUID(), "Outra pessoa", null, duplicateEmail, null, document,
                reloaded.passwordHash(), AccountRole.PACIENTE, reloaded.createdAt())));
        assertFalse(repository.findByEmail(duplicateEmail).isPresent());
    }

    private UUID register(String name, String email, String document, String role) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", name, "email", email, "document", document,
                                "password", "senha1234", "role", role))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private int count(String table, String column, UUID id) {
        return jdbc.queryForObject("select count(*) from " + table + " where " + column + " = ?", Integer.class, id);
    }
}
