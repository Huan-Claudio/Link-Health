package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.domain.MealRecord;
import br.edu.pucgoias.linkhealth.domain.MealRecordRepository;
import br.edu.pucgoias.linkhealth.domain.PatientRecordLock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MealRecordService {

    private final MealRecordRepository repository;
    private final UserAccountService accounts;
    private final PatientAccessService access;
    private final PatientPlannedMealsService plannedMeals;
    private final PatientRecordLock lock;

    public MealRecordService(MealRecordRepository repository, UserAccountService accounts,
            PatientAccessService access, PatientPlannedMealsService plannedMeals, PatientRecordLock lock) {
        this.repository = repository;
        this.accounts = accounts;
        this.access = access;
        this.plannedMeals = plannedMeals;
        this.lock = lock;
    }

    public List<MealRecord> list(UUID patientId) {
        access.ensureCanAccess(patientId);
        accounts.requirePatient(patientId);
        return repository.findByPatientId(patientId);
    }

    @Transactional
    public synchronized MealRecord create(
            UUID patientId,
            UUID mealPlanItemId,
            String mealName,
            Instant consumedAt,
            String notes) {
        prepareWrite(patientId);
        if (mealPlanItemId != null && plannedMeals.list(patientId).stream().noneMatch(m -> m.id().equals(mealPlanItemId))) {
            throw new InvalidRequestException("A refeição informada não pertence a um plano ativo deste paciente.");
        }
        Instant now = Instant.now();
        Instant effectiveConsumedAt = defaultToNow(consumedAt);
        requireUniqueCompletion(patientId, null, mealPlanItemId, effectiveConsumedAt);
        MealRecord record = new MealRecord(
                UUID.randomUUID(), patientId, mealPlanItemId, mealName.trim(), effectiveConsumedAt,
                trimToNull(notes), now, now);
        return repository.save(record);
    }

    @Transactional
    public synchronized MealRecord update(
            UUID patientId,
            UUID recordId,
            String mealName,
            Instant consumedAt,
            String notes) {
        prepareWrite(patientId);
        if (mealName == null && consumedAt == null && notes == null) {
            throw new InvalidRequestException("Informe ao menos um campo para atualizar a refeição.");
        }

        MealRecord current = find(patientId, recordId);
        requireUniqueCompletion(patientId, recordId, current.mealPlanItemId(),
                consumedAt == null ? current.consumedAt() : consumedAt);
        MealRecord updated = new MealRecord(
                current.id(), current.patientId(), current.mealPlanItemId(),
                mealName == null ? current.mealName() : mealName.trim(),
                consumedAt == null ? current.consumedAt() : consumedAt,
                notes == null ? current.notes() : trimToNull(notes),
                current.createdAt(), Instant.now());
        return repository.save(updated);
    }

    @Transactional
    public synchronized void delete(UUID patientId, UUID recordId) {
        prepareWrite(patientId);
        repository.delete(find(patientId, recordId));
    }

    private void prepareWrite(UUID patientId) {
        access.ensureCanWriteRecords(patientId);
        accounts.requirePatient(patientId);
        lock.lock(patientId);
    }

    private void requireUniqueCompletion(UUID patientId, UUID ignoredId, UUID plannedMealId, Instant consumedAt) {
        if (plannedMealId == null) return;
        var zone = RecordDates.zone(RecordDates.DEFAULT_ZONE);
        var day = consumedAt.atZone(zone).toLocalDate();
        if (repository.findByPatientId(patientId).stream().anyMatch(r -> !r.id().equals(ignoredId)
                && plannedMealId.equals(r.mealPlanItemId()) && r.consumedAt().atZone(zone).toLocalDate().equals(day))) {
            throw new InvalidRequestException("Esta refeição do plano já foi marcada como realizada neste dia.");
        }
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
