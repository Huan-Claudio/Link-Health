package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.domain.MealRecord;
import br.edu.pucgoias.linkhealth.domain.MealRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MealRecordService {

    private final MealRecordRepository repository;

    public MealRecordService(MealRecordRepository repository) {
        this.repository = repository;
    }

    public List<MealRecord> list(UUID patientId) {
        return repository.findByPatientId(patientId);
    }

    public MealRecord create(
            UUID patientId,
            UUID mealPlanItemId,
            String mealName,
            Instant consumedAt,
            String notes) {
        Instant now = Instant.now();
        MealRecord record = new MealRecord(
                UUID.randomUUID(), patientId, mealPlanItemId, mealName.trim(), defaultToNow(consumedAt),
                trimToNull(notes), now, now);
        return repository.save(record);
    }

    public MealRecord update(
            UUID patientId,
            UUID recordId,
            String mealName,
            Instant consumedAt,
            String notes) {
        if (mealName == null && consumedAt == null && notes == null) {
            throw new InvalidRequestException("Informe ao menos um campo para atualizar a refeição.");
        }

        MealRecord current = find(patientId, recordId);
        MealRecord updated = new MealRecord(
                current.id(), current.patientId(), current.mealPlanItemId(),
                mealName == null ? current.mealName() : mealName.trim(),
                consumedAt == null ? current.consumedAt() : consumedAt,
                notes == null ? current.notes() : trimToNull(notes),
                current.createdAt(), Instant.now());
        return repository.save(updated);
    }

    public void delete(UUID patientId, UUID recordId) {
        repository.delete(find(patientId, recordId));
    }

    private MealRecord find(UUID patientId, UUID recordId) {
        return repository.findByIdAndPatientId(recordId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de refeição não encontrado."));
    }

    private Instant defaultToNow(Instant value) {
        return value == null ? Instant.now() : value;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
