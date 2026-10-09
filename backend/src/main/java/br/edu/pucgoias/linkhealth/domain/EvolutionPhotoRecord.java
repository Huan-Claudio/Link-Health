package br.edu.pucgoias.linkhealth.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadados de uma foto de evolução corporal.
 *
 * <p>Não contém bytes da imagem. Quando a imagem já foi enviada, storageKey
 * identifica o arquivo no armazenamento interno, sem expor seu caminho pela API.</p>
 */
public record EvolutionPhotoRecord(
        UUID id,
        UUID patientId,
        String originalFileName,
        String contentType,
        Instant capturedAt,
        String notes,
        PhotoUploadStatus uploadStatus,
        String storageKey,
        Instant createdAt,
        Instant updatedAt) {
}
