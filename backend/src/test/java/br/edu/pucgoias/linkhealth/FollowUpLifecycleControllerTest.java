package br.edu.pucgoias.linkhealth;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "link-health.security.api-key=link-health-test-api-key-12345678901234567890",
        "link-health.photos.encryption-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
})
@AutoConfigureMockMvc
class FollowUpLifecycleControllerTest {
    private static final String API_KEY = "link-health-test-api-key-12345678901234567890";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void acceptsUpdatesAndDeactivatesFollowUpThroughHttp() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String documentSuffix = suffix.substring(0, 8);
        String nutritionistId = register("Ana", "ana-" + suffix + "@exemplo.com", "CRN-" + documentSuffix,
                "NUTRICIONISTA");
        String patientId = register("Maria", "maria-" + suffix + "@exemplo.com", documentSuffix,
                "PACIENTE");

        String invitationJson = mockMvc.perform(post("/api/v1/follow-ups")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nutritionistId", nutritionistId,
                                "patientId", patientId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDENTE")))
                .andReturn().getResponse().getContentAsString();
        String followUpId = objectMapper.readTree(invitationJson).get("id").asText();

        mockMvc.perform(post("/api/v1/follow-ups/{id}/response", followUpId)
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("patientId", patientId, "accept", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ATIVO")));

        mockMvc.perform(patch("/api/v1/follow-ups/{id}", followUpId)
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nutritionistId", nutritionistId,
                                "objective", "Melhorar a alimentação",
                                "waterGoalMl", 2000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waterGoalMl", is(2000)));

        mockMvc.perform(post("/api/v1/follow-ups/{id}/deactivate", followUpId)
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nutritionistId", nutritionistId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INATIVO")));

        mockMvc.perform(post("/api/v1/follow-ups/{id}/meal-plans", followUpId)
                        .header("X-API-Key", API_KEY))
                .andExpect(status().isBadRequest());
    }

    private String register(String fullName, String email, String document, String role) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", fullName,
                                "email", email,
                                "document", document,
                                "password", "senha1234",
                                "role", role))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(json);
        return response.get("id").asText();
    }
}
