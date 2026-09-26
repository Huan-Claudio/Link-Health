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

@SpringBootTest
@AutoConfigureMockMvc
class ShoppingAndProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldManageShoppingItemsForOnePatient() throws Exception {
        UUID patientId = UUID.randomUUID();
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
        UUID patientId = UUID.randomUUID();
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
