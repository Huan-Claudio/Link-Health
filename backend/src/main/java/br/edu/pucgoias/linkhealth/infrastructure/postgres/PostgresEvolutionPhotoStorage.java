package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoStorage;
import br.edu.pucgoias.linkhealth.infrastructure.security.PhotoEncryptionService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Armazena no PostgreSQL o conteúdo criptografado das fotos de evolução.
 */
@Component
@Profile("postgres")
public class PostgresEvolutionPhotoStorage implements EvolutionPhotoStorage {

    private final JdbcTemplate jdbcTemplate;
    private final PhotoEncryptionService encryptionService;

    public PostgresEvolutionPhotoStorage(
            JdbcTemplate jdbcTemplate,
            PhotoEncryptionService encryptionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.encryptionService = encryptionService;
    }

    @Override
    public String store(UUID patientId, UUID photoId, String extension, InputStream content) throws IOException {
        byte[] encrypted = encryptionService.encrypt(content);
        jdbcTemplate.update(
                """
                INSERT INTO evolution_photo_files (photo_id, encrypted_content, encryption_version, created_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (photo_id) DO UPDATE
                SET encrypted_content = EXCLUDED.encrypted_content,
                    encryption_version = EXCLUDED.encryption_version,
                    created_at = EXCLUDED.created_at
                """,
                photoId,
                encrypted,
                encryptionService.currentVersion(),
                Instant.now());
        return photoId.toString();
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        try {
            byte[] encrypted = jdbcTemplate.queryForObject(
                    "SELECT encrypted_content FROM evolution_photo_files WHERE photo_id = ?",
                    byte[].class,
                    parsePhotoId(storageKey));
            if (encrypted == null) {
                throw new NoSuchFileException(storageKey);
            }
            return new ByteArrayInputStream(encryptionService.decrypt(encrypted));
        } catch (EmptyResultDataAccessException exception) {
            throw new NoSuchFileException(storageKey);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        jdbcTemplate.update(
                "DELETE FROM evolution_photo_files WHERE photo_id = ?",
                parsePhotoId(storageKey));
    }

    private UUID parsePhotoId(String storageKey) throws IOException {
        try {
            return UUID.fromString(storageKey);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Chave de arquivo de foto inválida.", exception);
        }
    }
}
