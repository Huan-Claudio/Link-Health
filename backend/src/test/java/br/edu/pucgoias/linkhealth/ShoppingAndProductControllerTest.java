package br.edu.pucgoias.linkhealth;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "link-health.security.enabled=false",
        "link-health.photos.encryption-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
})
@AutoConfigureMockMvc
class ShoppingAndProductControllerTest {

    @Autowired
    private br.edu.pucgoias.linkhealth.service.UserAccountService accounts;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRequirePurchaseStatusAndPreserveExistingValue() throws Exception {
        UUID patient = PatientFixtures.create(accounts);
        String base = "/api/v1/patients/" + patient + "/shopping-items";
        String created = mockMvc.perform(post(base).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aveia\",\"quantity\":1,\"unit\":\"UNIT\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");
        String path = base + "/" + id + "/purchase-status";
        mockMvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content("{\"purchased\":true}"))
                .andExpect(status().isOk());
        for (String body : new String[]{"{}", "{\"purchased\":null}"}) {
            mockMvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(get(base)).andExpect(status().isOk()).andExpect(jsonPath("$[0].purchased", is(true)));
        mockMvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content("{\"purchased\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.purchased", is(false)));
    }

    @Test
    void shouldRejectOverflowFractionDigitsAndBlankNames() throws Exception {
        UUID patient = PatientFixtures.create(accounts);
        String shopping = "/api/v1/patients/" + patient + "/shopping-items";
        for (String value : new String[]{"1000000000", "0.0001", "999999999999999"}) {
            mockMvc.perform(post(shopping).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Aveia\",\"quantity\":" + value + ",\"unit\":\"UNIT\"}"))
                    .andExpect(status().isBadRequest());
        }
        String products = "/api/v1/patients/" + patient + "/recommended-products";
        for (String value : new String[]{"10000000000", "1.234", "-1"}) {
            mockMvc.perform(post(products).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Iogurte\",\"price\":" + value + "}"))
                    .andExpect(status().isBadRequest());
        }
        String created = mockMvc.perform(post(products).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Iogurte\",\"price\":2,\"currency\":\"USD\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");
        mockMvc.perform(patch(products + "/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(products + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Sem açúcar\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currency", is("USD")));
    }

    @Test
    void shouldRejectUnsupportedLinksAndUnknownPatients() throws Exception {
        UUID patient = PatientFixtures.create(accounts);
        String products = "/api/v1/patients/" + patient + "/recommended-products";
        for (String url : new String[]{"ftp://example.org/item", "https://user:pass@example.org/item"}) {
            mockMvc.perform(post(products).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Iogurte\",\"purchaseUrl\":\"" + url + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(get("/api/v1/patients/not-a-uuid/shopping-items"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("INVALID_PARAMETER")));
        UUID unknown = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/patients/" + unknown + "/recommended-products")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Iogurte\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/patients/" + unknown + "/shopping-items"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldManageShoppingItemsForOnePatient() throws Exception {
        UUID patientId = PatientFixtures.create(accounts);
        String basePath = "/api/v1/patients/" + patientId + "/shopping-items";

        MvcResult created = mockMvc.perform(post(basePath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aveia em flocos\",\"quantity\":2,\"unit\":\"PACKAGE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Aveia em flocos")))
                .andExpect(jsonPath("$.purchased", is(false)))
                .andReturn();

        String itemId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(patch(basePath + "/" + itemId + "/purchase-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purchased\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchased", is(true)));

        mockMvc.perform(get(basePath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete(basePath + "/" + itemId))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldManageRecommendedProductsForOnePatient() throws Exception {
        UUID patientId = PatientFixtures.create(accounts);
        String basePath = "/api/v1/patients/" + patientId + "/recommended-products";

        MvcResult created = mockMvc.perform(post(basePath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Iogurte natural\",\"price\":8.99,\"purchaseUrl\":\"https://exemplo.com/iogurte\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency", is("BRL")))
                .andReturn();

        String productId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(patch(basePath + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Sem açúcar adicionado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Sem açúcar adicionado")));

        mockMvc.perform(delete(basePath + "/" + productId))
                .andExpect(status().isNoContent());
    }
}
