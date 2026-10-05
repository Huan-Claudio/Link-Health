package br.edu.pucgoias.linkhealth;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest(properties = {
        "link-health.security.enabled=false",
        "link-health.photos.encryption-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=",
        "link-health.photos.storage-directory=${java.io.tmpdir}/link-health-test-evolution-photos"
})
@AutoConfigureMockMvc
class EvolutionPhotoRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateUploadReadAndDeleteEvolutionPhoto() throws Exception {
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
        mockMvc.perform(multipart(basePath + "/" + photoId + "/content")
                        .file(new MockMultipartFile(
                                "file",
                                "evolucao-frontal.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                validJpeg())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadStatus", is("STORED")));

        mockMvc.perform(get(basePath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uploadStatus", is("STORED")));
        mockMvc.perform(get(basePath + "/" + photoId + "/content"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
        mockMvc.perform(delete(basePath + "/" + photoId))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectAFileThatIsNotARealImage() throws Exception {
        UUID patientId = UUID.randomUUID();
        String basePath = "/api/v1/patients/" + patientId + "/evolution-photos";

        MvcResult created = mockMvc.perform(post(basePath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalFileName": "evolucao-frontal.jpg",
                                  "contentType": "image/jpeg"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String photoId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(multipart(basePath + "/" + photoId + "/content")
                        .file(new MockMultipartFile(
                                "file",
                                "arquivo-falso.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "isso não é uma imagem".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST")));
        mockMvc.perform(delete(basePath + "/" + photoId))
                .andExpect(status().isNoContent());
    }

    private byte[] validJpeg() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.GREEN.getRGB());
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", output);
            return output.toByteArray();
        }
    }
}
