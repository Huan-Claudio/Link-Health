package br.edu.pucgoias.linkhealth;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class EvolutionPhotoRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldManageEvolutionPhotoMetadataWithoutStoringTheImageYet() throws Exception {
        UUID patientId = UUID.randomUUID();
        String basePath = "/api/v1/patients/" + patientId + "/evolution-photos";

        MvcResult created = mockMvc.perform(post(basePath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalFileName": "evolucao-frontal.jpg",
                                  "contentType": "image/jpeg",
                                  "notes": "Foto frontal para acompanhamento"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFileName", is("evolucao-frontal.jpg")))
                .andExpect(jsonPath("$.uploadStatus", is("PENDING_UPLOAD")))
                .andReturn();

        String photoId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(get(basePath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(delete(basePath + "/" + photoId))
                .andExpect(status().isNoContent());
    }
}
