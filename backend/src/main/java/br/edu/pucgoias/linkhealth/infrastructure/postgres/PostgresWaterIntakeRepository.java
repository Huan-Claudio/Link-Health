package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.WaterIntake;
import br.edu.pucgoias.linkhealth.domain.WaterIntakeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class PostgresWaterIntakeRepository implements WaterIntakeRepository {

    private final WaterIntakeJpaSpringRepository repository;

    public PostgresWaterIntakeRepository(WaterIntakeJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<WaterIntake> findByPatientId(UUID patientId) {
        return repository.findAllByPatientIdOrderByConsumedAtAsc(patientId).stream().map(WaterIntakeEntity::toDomain).toList();
    }

    @Override
    public Optional<WaterIntake> findByIdAndPatientId(UUID intakeId, UUID patientId) {
        return repository.findByIdAndPatientId(intakeId, patientId).map(WaterIntakeEntity::toDomain);
    }

    @Override
    public WaterIntake save(WaterIntake intake) {
        return repository.save(WaterIntakeEntity.from(intake)).toDomain();
    }

    @Override
    public void delete(WaterIntake intake) {
        repository.deleteById(intake.id());
    }
}
