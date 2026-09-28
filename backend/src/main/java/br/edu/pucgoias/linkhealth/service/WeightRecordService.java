package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.domain.WeightRecord;
import br.edu.pucgoias.linkhealth.domain.WeightRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class WeightRecordService {

    private final WeightRecordRepository repository;

    public WeightRecordService(WeightRecordRepository repository) {
        this.repository = repository;
    }

    public List<WeightRecord> list(UUID patientId) {
        return repository.findByPatientId(patientId);
    }

    public WeightRecord create(UUID patientId, BigDecimal weightKg, Instant measuredAt, String notes) {
        Instant now = Instant.now();
        WeightRecord record = new WeightRecord(
                UUID.randomUUID(), patientId, weightKg, defaultToNow(measuredAt), trimToNull(notes), now, now);
        return repository.save(record);
    }

    public WeightRecord update(
            UUID patientId,
            UUID recordId,
            BigDecimal weightKg,
            Instant measuredAt,
            String notes) {
        if (weightKg == null && measuredAt == null && notes == null) {
            throw new InvalidRequestException("Informe ao menos um campo para atualizar o peso.");
        }

        WeightRecord current = find(patientId, recordId);
        WeightRecord updated = new WeightRecord(
                current.id(), current.patientId(),
                weightKg == null ? current.weightKg() : weightKg,
                measuredAt == null ? current.measuredAt() : measuredAt,
                notes == null ? current.notes() : trimToNull(notes),
                current.createdAt(), Instant.now());
        return repository.save(updated);
    }

    public void delete(UUID patientId, UUID recordId) {
        repository.delete(find(patientId, recordId));
    }

    private WeightRecord find(UUID patientId, UUID recordId) {
        return repository.findByIdAndPatientId(recordId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de peso não encontrado."));
    }

    private Instant defaultToNow(Instant value) {
        return value == null ? Instant.now() : value;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
