package br.edu.pucgoias.linkhealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.FollowUp;
import br.edu.pucgoias.linkhealth.domain.FollowUpRepository;
import br.edu.pucgoias.linkhealth.domain.FollowUpStatus;
import br.edu.pucgoias.linkhealth.service.UserAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class PostgresFollowUpPersistenceTest {
    private static final String API_KEY = "link-health-test-api-key-12345678901234567890";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FollowUpRepository repository;

    @Autowired
    private UserAccountService accounts;

    @Test
    void persistsTheInvitationAcceptanceDetailsAndInactiveHistoryThroughHttp() throws Exception {
        UUID nutritionistId = account(AccountRole.NUTRICIONISTA);
        UUID patientId = account(AccountRole.PACIENTE);
        UUID id = invite(nutritionistId, patientId);
        assertEquals(FollowUpStatus.PENDENTE, repository.findById(id).orElseThrow().status());

        mockMvc.perform(post("/api/v1/follow-ups/{id}/response", id)
                        .header("X-API-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("patientId", patientId, "accept", true))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ATIVO"));
        mockMvc.perform(patch("/api/v1/follow-ups/{id}", id)
                        .header("X-API-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nutritionistId", nutritionistId, "objective", "x".repeat(101)))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/follow-ups/{id}", id)
                        .header("X-API-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nutritionistId", nutritionistId, "objective", "Melhorar alimentação", "waterGoalMl", 2000))))
                .andExpect(status().isOk());
        FollowUp updated = repository.findById(id).orElseThrow();
        assertEquals("Melhorar alimentação", updated.objective());
        assertEquals(2000, updated.waterGoalMl());

        mockMvc.perform(post("/api/v1/follow-ups/{id}/deactivate", id)
                        .header("X-API-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nutritionistId", nutritionistId))))
                .andExpect(status().isOk());
        assertEquals(FollowUpStatus.INATIVO, repository.findById(id).orElseThrow().status());
        UUID secondId = invite(nutritionistId, patientId);
        mockMvc.perform(post("/api/v1/follow-ups/{id}/response", secondId)
                        .header("X-API-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("patientId", patientId, "accept", false))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RECUSADO"));
        invite(nutritionistId, patientId);
        mockMvc.perform(get("/api/v1/patients/{id}/follow-ups", patientId).header("X-API-Key", API_KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mockMvc.perform(get("/api/v1/nutritionists/{id}/follow-ups", nutritionistId).header("X-API-Key", API_KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void databaseRejectsInvalidProfilesGoalsAndDuplicateOpenFollowUps() {
        UUID nutritionistId = account(AccountRole.NUTRICIONISTA);
        UUID patientId = account(AccountRole.PACIENTE);
        assertThrows(InvalidRequestException.class, () -> repository.save(followUp(patientId, nutritionistId, null)));
        assertThrows(InvalidRequestException.class, () -> repository.save(followUp(nutritionistId, UUID.randomUUID(), null)));
        assertThrows(InvalidRequestException.class, () -> repository.save(followUp(nutritionistId, patientId, 0)));
        assertEquals(0, repository.findByPatientId(patientId).size());
        repository.save(followUp(nutritionistId, patientId, 2000));
        assertThrows(InvalidRequestException.class, () -> repository.save(followUp(nutritionistId, patientId, 2000)));
        assertEquals(1, repository.findByPatientId(patientId).size());
    }

    private FollowUp followUp(UUID nutritionistId, UUID patientId, Integer waterGoalMl) {
        return new FollowUp(UUID.randomUUID(), nutritionistId, patientId, FollowUpStatus.PENDENTE,
                null, waterGoalMl, Instant.now());
    }

    private UUID account(AccountRole role) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return accounts.register("Pessoa " + suffix, null, suffix + "@exemplo.com", null,
                role.name() + "-" + suffix, "senha1234", role).id();
    }

    private UUID invite(UUID nutritionistId, UUID patientId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/follow-ups")
                        .header("X-API-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nutritionistId", nutritionistId, "patientId", patientId))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }
}
