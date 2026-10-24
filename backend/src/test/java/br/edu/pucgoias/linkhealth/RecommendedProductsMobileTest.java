package br.edu.pucgoias.linkhealth;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.FollowUp;
import br.edu.pucgoias.linkhealth.domain.UserAccount;
import br.edu.pucgoias.linkhealth.service.FollowUpService;
import br.edu.pucgoias.linkhealth.service.UserAccountService;
import com.jayway.jsonpath.JsonPath;
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
class RecommendedProductsMobileTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountService accounts;
    @Autowired FollowUpService followUps;

    @Test
    void recommendationLifecycleRequiresTheCorrectActiveRelationship() throws Exception {
        UserAccount patient = account(AccountRole.PACIENTE);
        UserAccount stranger = account(AccountRole.PACIENTE);
        UserAccount nutritionist = account(AccountRole.NUTRICIONISTA);
        UserAccount outsider = account(AccountRole.NUTRICIONISTA);
        String patientToken = login(patient);
        String strangerToken = login(stranger);
        String nutritionistToken = login(nutritionist);
        String outsiderToken = login(outsider);
        String base = "/api/v1/patients/" + patient.id() + "/recommended-products";
        String selector = "/api/v1/nutritionists/me/recommended-product-patients";
        String body = "{\"name\":\"Iogurte\",\"price\":8.99,\"purchaseUrl\":\"https://example.org/item\"}";

        FollowUp pending = followUps.invite(nutritionist.id(), patient.id(), null, null);
        mvc.perform(get(selector).header("Authorization", "Bearer " + nutritionistToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(post(base).header("Authorization", "Bearer " + nutritionistToken)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        followUps.respond(pending.id(), patient.id(), true);
        mvc.perform(get(selector).header("Authorization", "Bearer " + nutritionistToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id", is(patient.id().toString())));
        mvc.perform(get(selector).header("Authorization", "Bearer " + patientToken)).andExpect(status().isForbidden());
        mvc.perform(post(base).header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post(base).header("Authorization", "Bearer " + outsiderToken)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        String response = mvc.perform(post(base).header("Authorization", "Bearer " + nutritionistToken)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(response, "$.id");
        mvc.perform(get(base).header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name", is("Iogurte")));
        mvc.perform(get(base).header("Authorization", "Bearer " + strangerToken)).andExpect(status().isForbidden());
        mvc.perform(patch(base + "/" + id).header("Authorization", "Bearer " + nutritionistToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Iogurte natural\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name", is("Iogurte natural")));
        mvc.perform(delete(base + "/" + id).header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
        mvc.perform(delete(base + "/" + id).header("Authorization", "Bearer " + nutritionistToken))
                .andExpect(status().isNoContent());
        followUps.deactivate(pending.id(), nutritionist.id());
        mvc.perform(get(base).header("Authorization", "Bearer " + nutritionistToken)).andExpect(status().isForbidden());
    }

    private UserAccount account(AccountRole role) {
        String id = UUID.randomUUID().toString();
        return accounts.register("Pessoa teste", null, id + "@example.org", null, id.substring(0, 20), "senha1234", role);
    }
    private String login(UserAccount account) throws Exception {
        String body = "{\"email\":\"" + account.email() + "\",\"password\":\"senha1234\"}";
        String result = mvc.perform(post("/api/v1/auth/mobile-session").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(result, "$.accessToken");
    }
}
