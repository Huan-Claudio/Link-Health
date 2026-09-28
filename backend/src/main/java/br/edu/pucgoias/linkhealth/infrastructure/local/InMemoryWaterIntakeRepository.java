package br.edu.pucgoias.linkhealth.infrastructure.local;

import br.edu.pucgoias.linkhealth.domain.WaterIntake;
import br.edu.pucgoias.linkhealth.domain.WaterIntakeRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryWaterIntakeRepository implements WaterIntakeRepository {

    private final ConcurrentMap<UUID, WaterIntake> intakes = new ConcurrentHashMap<>();

    @Override
    public List<WaterIntake> findByPatientId(UUID patientId) {
        return intakes.values().stream()
                .filter(intake -> intake.patientId().equals(patientId))
                .sorted(Comparator.comparing(WaterIntake::consumedAt))
                .toList();
    }

    @Override
    public Optional<WaterIntake> findByIdAndPatientId(UUID intakeId, UUID patientId) {
        return Optional.ofNullable(intakes.get(intakeId))
                .filter(intake -> intake.patientId().equals(patientId));
    }

    @Override
    public WaterIntake save(WaterIntake intake) {
        intakes.put(intake.id(), intake);
        return intake;
    }

    @Override
    public void delete(WaterIntake intake) {
        intakes.remove(intake.id());
    }
}
