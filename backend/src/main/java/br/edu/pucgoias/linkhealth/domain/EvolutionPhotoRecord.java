package br.edu.pucgoias.linkhealth.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadados de uma foto de evolução corporal.
 *
 * <p>Não contém bytes da imagem. A imagem será enviada e armazenada em uma
 * etapa posterior, depois da definição da estratégia de armazenamento.</p>
 */
public record EvolutionPhotoRecord(
        UUID id,
        UUID patientId,
        String originalFileName,
        String contentType,
        Instant capturedAt,
        String notes,
        PhotoUploadStatus uploadStatus,
        Instant createdAt,
        Instant updatedAt) {
}
