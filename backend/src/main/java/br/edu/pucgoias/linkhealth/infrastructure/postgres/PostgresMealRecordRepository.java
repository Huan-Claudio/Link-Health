package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.MealRecord;
import br.edu.pucgoias.linkhealth.domain.MealRecordRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class PostgresMealRecordRepository implements MealRecordRepository {

    private final MealRecordJpaSpringRepository repository;

    public PostgresMealRecordRepository(MealRecordJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MealRecord> findByPatientId(UUID patientId) {
        return repository.findAllByPatientIdOrderByConsumedAtAsc(patientId).stream().map(MealRecordEntity::toDomain).toList();
    }

    @Override
    public Optional<MealRecord> findByIdAndPatientId(UUID recordId, UUID patientId) {
        return repository.findByIdAndPatientId(recordId, patientId).map(MealRecordEntity::toDomain);
    }

    @Override
    public MealRecord save(MealRecord record) {
        return repository.save(MealRecordEntity.from(record)).toDomain();
    }

    @Override
    public void delete(MealRecord record) {
        repository.deleteById(record.id());
    }
}
