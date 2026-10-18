package br.edu.pucgoias.linkhealth;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.UserAccount;
import br.edu.pucgoias.linkhealth.service.UserAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "link-health.security.api-key=link-health-test-api-key-12345678901234567890",
        "link-health.photos.encryption-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
})
@AutoConfigureMockMvc
class ShoppingMobileSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountService accounts;

    @Test
    void shouldLetPatientReadAndUpdateOnlyTheirOwnShoppingList() throws Exception {
        UserAccount patient = patient("lista-" + UUID.randomUUID());
        String token = login(patient.email());
        String basePath = "/api/v1/patients/" + patient.id() + "/shopping-items";

        MvcResult created = mockMvc.perform(post(basePath)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aveia\",\"quantity\":2,\"unit\":\"PACKAGE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Aveia")))
                .andExpect(jsonPath("$.purchased", is(false)))
                .andReturn();

        String itemId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(get(basePath).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(itemId)));

        mockMvc.perform(patch(basePath + "/" + itemId + "/purchase-status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purchased\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchased", is(true)));

        mockMvc.perform(get("/api/v1/patients/{patientId}/shopping-items", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    private UserAccount patient(String suffix) {
        return accounts.register("Paciente de Teste", null, suffix + "@exemplo.com", null,
                suffix.substring(0, 11), "senha1234", AccountRole.PACIENTE);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/mobile-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"senha1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
