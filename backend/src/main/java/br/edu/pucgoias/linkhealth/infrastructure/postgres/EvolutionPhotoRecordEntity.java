package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoRecord;
import br.edu.pucgoias.linkhealth.domain.PhotoUploadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evolution_photo_records")
public class EvolutionPhotoRecordEntity {

    @Id
    private UUID id;
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;
    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;
    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
    @Column(length = 500)
    private String notes;
    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 30)
    private PhotoUploadStatus uploadStatus;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EvolutionPhotoRecordEntity() {
    }

    private EvolutionPhotoRecordEntity(EvolutionPhotoRecord record) {
        this.id = record.id();
        this.patientId = record.patientId();
        this.originalFileName = record.originalFileName();
        this.contentType = record.contentType();
        this.capturedAt = record.capturedAt();
        this.notes = record.notes();
        this.uploadStatus = record.uploadStatus();
        this.createdAt = record.createdAt();
        this.updatedAt = record.updatedAt();
    }

    static EvolutionPhotoRecordEntity from(EvolutionPhotoRecord record) {
        return new EvolutionPhotoRecordEntity(record);
    }

    EvolutionPhotoRecord toDomain() {
        return new EvolutionPhotoRecord(
                id,
                patientId,
                originalFileName,
                contentType,
                capturedAt,
                notes,
                uploadStatus,
                createdAt,
                updatedAt);
    }
}
