package br.edu.pucgoias.linkhealth.api.evolution;

import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoRecord;
import br.edu.pucgoias.linkhealth.domain.PhotoUploadStatus;
import java.time.Instant;
import java.util.UUID;

public record EvolutionPhotoRecordResponse(
        UUID id,
        UUID patientId,
        String originalFileName,
        String contentType,
        Instant capturedAt,
        String notes,
        PhotoUploadStatus uploadStatus,
        Instant createdAt,
        Instant updatedAt) {

    static EvolutionPhotoRecordResponse from(EvolutionPhotoRecord record) {
        return new EvolutionPhotoRecordResponse(
                record.id(),
                record.patientId(),
                record.originalFileName(),
                record.contentType(),
                record.capturedAt(),
                record.notes(),
                record.uploadStatus(),
                record.createdAt(),
                record.updatedAt());
    }
}
