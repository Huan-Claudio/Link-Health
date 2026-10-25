package br.edu.pucgoias.linkhealth;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.edu.pucgoias.linkhealth.service.UserAccountService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=${LINK_HEALTH_TEST_DB_URL}",
        "spring.datasource.username=${LINK_HEALTH_TEST_DB_USERNAME}",
        "spring.datasource.password=${LINK_HEALTH_TEST_DB_PASSWORD:}",
        "link-health.security.api-key=link-health-test-api-key-12345678901234567890",
        "link-health.photos.encryption-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
})
@ActiveProfiles("postgres")
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "LINK_HEALTH_TEST_DB_URL", matches = ".+")
class PostgresEvolutionPhotoPersistenceTest {
    private static final String KEY = "link-health-test-api-key-12345678901234567890";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired UserAccountService accounts;

    @Test
    void storesEncryptedPhotoThenDownloadsTheOriginalAndRemovesItsContent() throws Exception {
        UUID patient = PatientFixtures.create(accounts);
        String base = "/api/v1/patients/" + patient + "/evolution-photos";
        String response = mvc.perform(post(base).header("X-API-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalFileName\":\"teste.png\",\"contentType\":\"image/png\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID photo = UUID.fromString(JsonPath.read(response, "$.id"));
        entityManager.flush();
        byte[] original;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
            original = output.toByteArray();
        }
        mvc.perform(multipart(base + "/" + photo + "/content").file(
                        new MockMultipartFile("file", "teste.png", "image/png", original)).header("X-API-Key", KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.uploadStatus").value("STORED"));
        byte[] encrypted = jdbc.queryForObject(
                "SELECT encrypted_content FROM evolution_photo_files WHERE photo_id = ?", byte[].class, photo);
        assertNotNull(encrypted);
        assertFalse(Arrays.equals(original, encrypted));
        assertEquals(original.length + 29, encrypted.length);
        mvc.perform(get(base + "/" + photo + "/content").header("X-API-Key", KEY))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(original));
        mvc.perform(delete(base + "/" + photo).header("X-API-Key", KEY)).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM evolution_photo_files WHERE photo_id = ?", Integer.class, photo));
    }
}
