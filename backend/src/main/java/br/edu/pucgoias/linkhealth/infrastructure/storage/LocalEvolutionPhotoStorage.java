package br.edu.pucgoias.linkhealth.infrastructure.storage;

import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoStorage;
import br.edu.pucgoias.linkhealth.infrastructure.security.PhotoEncryptionService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Armazenamento local para desenvolvimento.
 *
 * <p>Os arquivos ficam fora do repositório e recebem nomes gerados pela API,
 * sem usar o nome que veio do celular como caminho de arquivo.</p>
 */
@Component
@Profile("local")
public class LocalEvolutionPhotoStorage implements EvolutionPhotoStorage {

    private final Path rootDirectory;
    private final PhotoEncryptionService encryptionService;

    public LocalEvolutionPhotoStorage(
            @Value("${link-health.photos.storage-directory}") String storageDirectory,
            PhotoEncryptionService encryptionService) {
        this.rootDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.encryptionService = encryptionService;
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível preparar a pasta de fotos de evolução.", exception);
        }
    }

    @Override
    public String store(UUID patientId, UUID photoId, String extension, InputStream content) throws IOException {
        String storageKey = patientId + "/" + photoId + ".enc";
        Path target = resolve(storageKey);
        Files.createDirectories(target.getParent());

        try {
            Files.write(target, encryptionService.encrypt(content));
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(target);
            throw exception;
        }
        return storageKey;
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return new ByteArrayInputStream(encryptionService.decrypt(Files.readAllBytes(resolve(storageKey))));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    private Path resolve(String storageKey) throws IOException {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IOException("A chave do arquivo está vazia.");
        }

        Path candidate = rootDirectory.resolve(storageKey).normalize();
        if (!candidate.startsWith(rootDirectory)) {
            throw new IOException("Caminho de arquivo inválido.");
        }
        return candidate;
    }
}
