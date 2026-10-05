package br.edu.pucgoias.linkhealth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.pucgoias.linkhealth.infrastructure.security.PhotoEncryptionService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PhotoEncryptionServiceTest {

    private static final String KEY = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=";

    @Test
    void shouldEncryptAndAuthenticatePhotoContent() throws Exception {
        PhotoEncryptionService service = new PhotoEncryptionService(KEY);
        byte[] original = "conteúdo confidencial da foto".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = service.encrypt(new ByteArrayInputStream(original));

        assertThat(encrypted).hasSizeGreaterThan(original.length);
        assertThat(service.decrypt(encrypted)).isEqualTo(original);

        encrypted[encrypted.length - 1] ^= 1;
        assertThatThrownBy(() -> service.decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class);
    }
}
