package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.domain.WaterIntake;
import br.edu.pucgoias.linkhealth.domain.WaterIntakeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class WaterIntakeService {

    private final WaterIntakeRepository repository;

    public WaterIntakeService(WaterIntakeRepository repository) {
        this.repository = repository;
    }

    public List<WaterIntake> list(UUID patientId, LocalDate date) {
        return repository.findByPatientId(patientId).stream()
                .filter(intake -> date == null || intake.consumedAt().atZone(ZoneOffset.UTC).toLocalDate().equals(date))
                .toList();
    }

    public WaterIntake create(UUID patientId, int amountMilliliters, Instant consumedAt) {
        Instant now = Instant.now();
        WaterIntake intake = new WaterIntake(
                UUID.randomUUID(), patientId, amountMilliliters, defaultToNow(consumedAt), now, now);
        return repository.save(intake);
    }

    public WaterIntake update(UUID patientId, UUID intakeId, Integer amountMilliliters, Instant consumedAt) {
        if (amountMilliliters == null && consumedAt == null) {
            throw new InvalidRequestException("Informe ao menos um campo para atualizar o registro de água.");
        }

        WaterIntake current = find(patientId, intakeId);
        WaterIntake updated = new WaterIntake(
                current.id(), current.patientId(),
                amountMilliliters == null ? current.amountMilliliters() : amountMilliliters,
                consumedAt == null ? current.consumedAt() : consumedAt,
                current.createdAt(), Instant.now());
        return repository.save(updated);
    }

    public void delete(UUID patientId, UUID intakeId) {
        repository.delete(find(patientId, intakeId));
    }

    private WaterIntake find(UUID patientId, UUID intakeId) {
        return repository.findByIdAndPatientId(intakeId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de água não encontrado."));
    }

    private Instant defaultToNow(Instant value) {
        return value == null ? Instant.now() : value;
    }
}
