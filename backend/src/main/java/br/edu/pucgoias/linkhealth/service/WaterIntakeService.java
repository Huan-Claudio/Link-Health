package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.domain.PatientRecordLock;
import br.edu.pucgoias.linkhealth.domain.WaterIntake;
import br.edu.pucgoias.linkhealth.domain.WaterIntakeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class WaterIntakeService {

    private final WaterIntakeRepository repository;
    private final UserAccountService accounts;
    private final PatientAccessService access;
    private final PatientRecordLock lock;

    public WaterIntakeService(
            WaterIntakeRepository repository,
            UserAccountService accounts,
            PatientAccessService access,
            PatientRecordLock lock) {
        this.repository = repository;
        this.accounts = accounts;
        this.access = access;
        this.lock = lock;
    }

    public List<WaterIntake> list(UUID patientId, LocalDate date) {
        return list(patientId, date, RecordDates.DEFAULT_ZONE);
    }

    public List<WaterIntake> list(UUID patientId, LocalDate date, String zoneId) {
        access.ensureCanAccess(patientId);
        accounts.requirePatient(patientId);
        ZoneId zone = RecordDates.zone(zoneId);
        return repository.findByPatientId(patientId).stream()
                .filter(
                        intake ->
                                date == null
                                        || intake.consumedAt()
                                                .atZone(zone)
                                                .toLocalDate()
                                                .equals(date))
                .toList();
    }

    @Transactional
    public synchronized WaterIntake create(
            UUID patientId, int amountMilliliters, Instant consumedAt) {
        prepareWrite(patientId);
        Instant now = Instant.now();
        WaterIntake intake =
                new WaterIntake(
                        UUID.randomUUID(),
                        patientId,
                        amountMilliliters,
                        defaultToNow(consumedAt),
                        now,
                        now);
        return repository.save(intake);
    }

    @Transactional
    public synchronized WaterIntake update(
            UUID patientId, UUID intakeId, Integer amountMilliliters, Instant consumedAt) {
        prepareWrite(patientId);
        if (amountMilliliters == null && consumedAt == null) {
            throw new InvalidRequestException(
                    "Informe ao menos um campo para atualizar o registro de água.");
        }

        WaterIntake current = find(patientId, intakeId);
        WaterIntake updated =
                new WaterIntake(
                        current.id(),
                        current.patientId(),
                        amountMilliliters == null ? current.amountMilliliters() : amountMilliliters,
                        consumedAt == null ? current.consumedAt() : consumedAt,
                        current.createdAt(),
                        Instant.now());
        return repository.save(updated);
    }

    @Transactional
    public synchronized void delete(UUID patientId, UUID intakeId) {
        prepareWrite(patientId);
        repository.delete(find(patientId, intakeId));
    }

    @Transactional
    public synchronized List<WaterIntake> adjust(
            UUID patientId, int amount, LocalDate date, String zoneId) {
        prepareWrite(patientId);
        ZoneId zone = RecordDates.zone(zoneId);
        LocalDate today = LocalDate.now(zone);
        if (amount == 0
                || amount < -10000
                || amount > 10000
                || date == null
                || date.isAfter(today)) {
            throw new InvalidRequestException(
                    "Informe uma quantidade entre -10000 e 10000 ml, diferente de zero, e uma data"
                        + " não futura.");
        }
        List<WaterIntake> entries = list(patientId, date, zoneId);
        if (amount > 0) {
            Instant now = Instant.now();
            Instant consumedAt =
                    date.equals(today) ? now : date.atTime(12, 0).atZone(zone).toInstant();
            repository.save(
                    new WaterIntake(UUID.randomUUID(), patientId, amount, consumedAt, now, now));
        } else {
            long total = entries.stream().mapToLong(WaterIntake::amountMilliliters).sum();
            if (total < -(long) amount) {
                throw new InvalidRequestException(
                        "A quantidade a remover é maior que o consumo registrado neste dia.");
            }
            int remaining = -amount;
            for (WaterIntake entry :
                    entries.stream()
                            .sorted(
                                    Comparator.comparing(WaterIntake::consumedAt)
                                            .thenComparing(WaterIntake::id)
                                            .reversed())
                            .toList()) {
                if (remaining == 0) break;
                int removed = Math.min(remaining, entry.amountMilliliters());
                if (removed == entry.amountMilliliters()) repository.delete(entry);
                else
                    repository.save(
                            new WaterIntake(
                                    entry.id(),
                                    patientId,
                                    entry.amountMilliliters() - removed,
                                    entry.consumedAt(),
                                    entry.createdAt(),
                                    Instant.now()));
                remaining -= removed;
            }
        }
        return list(patientId, date, zoneId);
    }

    private void prepareWrite(UUID patientId) {
        access.ensureCanWriteRecords(patientId);
        accounts.requirePatient(patientId);
        lock.lock(patientId);
    }

    @Transactional
    public synchronized List<WaterIntake> setDailyTotal(
            UUID patient, int target, LocalDate date, String zoneId) {
        prepareWrite(patient);
        ZoneId zone = RecordDates.zone(zoneId);
        if (target < 0 || target > 50000 || date == null || date.isAfter(LocalDate.now(zone)))
            throw new InvalidRequestException(
                    "Informe um total válido de até 50000 ml para um dia não futuro.");
        List<WaterIntake> entries = list(patient, date, zoneId);
        long total = entries.stream().mapToLong(WaterIntake::amountMilliliters).sum();
        long difference = target - total;
        Instant now = Instant.now();
        if (difference > 0) {
            Instant at =
                    date.equals(LocalDate.now(zone))
                            ? now
                            : date.atTime(12, 0).atZone(zone).toInstant();
            while (difference > 0) {
                int amount = (int) Math.min(10000, difference);
                repository.save(new WaterIntake(UUID.randomUUID(), patient, amount, at, now, now));
                difference -= amount;
            }
        } else {
            long remaining = -difference;
            for (var entry :
                    entries.stream()
                            .sorted(
                                    Comparator.comparing(WaterIntake::consumedAt)
                                            .thenComparing(WaterIntake::id)
                                            .reversed())
                            .toList()) {
                if (remaining == 0) break;
                int removed = (int) Math.min(remaining, entry.amountMilliliters());
                if (removed == entry.amountMilliliters()) repository.delete(entry);
                else
                    repository.save(
                            new WaterIntake(
                                    entry.id(),
                                    patient,
                                    entry.amountMilliliters() - removed,
                                    entry.consumedAt(),
                                    entry.createdAt(),
                                    now));
                remaining -= removed;
            }
        }
        return list(patient, date, zoneId);
    }

    private WaterIntake find(UUID patientId, UUID intakeId) {
        return repository
                .findByIdAndPatientId(intakeId, patientId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Registro de água não encontrado."));
    }

    private Instant defaultToNow(Instant value) {
        return value == null ? Instant.now() : value;
    }
}
