package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoRecord;
import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoRecordRepository;
import br.edu.pucgoias.linkhealth.domain.PhotoUploadStatus;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EvolutionPhotoRecordService {

    private final EvolutionPhotoRecordRepository repository;

    public EvolutionPhotoRecordService(EvolutionPhotoRecordRepository repository) {
        this.repository = repository;
    }

    public List<EvolutionPhotoRecord> list(UUID patientId) {
        return repository.findByPatientId(patientId);
    }

    public EvolutionPhotoRecord create(
            UUID patientId, String originalFileName, String contentType, Instant capturedAt, String notes) {
        Instant now = Instant.now();
        EvolutionPhotoRecord record = new EvolutionPhotoRecord(
                UUID.randomUUID(),
                patientId,
                originalFileName.trim(),
                contentType.toLowerCase(Locale.ROOT),
                capturedAt == null ? now : capturedAt,
                trimToNull(notes),
                PhotoUploadStatus.PENDING_UPLOAD,
                now,
                now);
        return repository.save(record);
    }

    public void delete(UUID patientId, UUID photoId) {
        repository.delete(find(patientId, photoId));
    }

    private EvolutionPhotoRecord find(UUID patientId, UUID photoId) {
        return repository.findByIdAndPatientId(photoId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de foto de evolução não encontrado."));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
