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
class PatientRecordsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldManageWaterIntakes() throws Exception {
        UUID patientId = UUID.randomUUID();
        String basePath = "/api/v1/patients/" + patientId + "/water-intakes";
        MvcResult created = mockMvc.perform(post(basePath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMilliliters\":500}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountMilliliters", is(500)))
                .andReturn();

        String intakeId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(patch(basePath + "/" + intakeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMilliliters\":750}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountMilliliters", is(750)));
        mockMvc.perform(delete(basePath + "/" + intakeId)).andExpect(status().isNoContent());
    }

    @Test
    void shouldManageMealRecords() throws Exception {
        UUID patientId = UUID.randomUUID();
        String basePath = "/api/v1/patients/" + patientId + "/meal-records";
        MvcResult created = mockMvc.perform(post(basePath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealName\":\"Café da manhã\",\"notes\":\"Realizada no horário\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mealName", is("Café da manhã")))
                .andReturn();

        String recordId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(get(basePath)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(delete(basePath + "/" + recordId)).andExpect(status().isNoContent());
    }

    @Test
    void shouldManageWeightRecords() throws Exception {
        UUID patientId = UUID.randomUUID();
        String basePath = "/api/v1/patients/" + patientId + "/weight-records";
        MvcResult created = mockMvc.perform(post(basePath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":72.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weightKg", is(72.5)))
                .andReturn();

        String recordId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(patch(basePath + "/" + recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":71.80}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightKg", is(71.8)));
        mockMvc.perform(delete(basePath + "/" + recordId)).andExpect(status().isNoContent());
    }
}
