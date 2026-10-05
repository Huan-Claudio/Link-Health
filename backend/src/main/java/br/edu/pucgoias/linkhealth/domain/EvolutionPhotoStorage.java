package br.edu.pucgoias.linkhealth.domain;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Porta para guardar os arquivos de fotos de evolução.
 *
 * <p>A aplicação depende desta interface, e não de uma pasta específica.
 * Isso permite substituir o armazenamento local por um serviço de nuvem no futuro.</p>
 */
public interface EvolutionPhotoStorage {

    String store(UUID patientId, UUID photoId, String extension, InputStream content) throws IOException;

    InputStream open(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;
}
