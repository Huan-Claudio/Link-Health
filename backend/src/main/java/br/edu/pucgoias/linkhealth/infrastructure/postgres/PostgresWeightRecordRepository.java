package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.WeightRecord;
import br.edu.pucgoias.linkhealth.domain.WeightRecordRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class PostgresWeightRecordRepository implements WeightRecordRepository {

    private final WeightRecordJpaSpringRepository repository;

    public PostgresWeightRecordRepository(WeightRecordJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<WeightRecord> findByPatientId(UUID patientId) {
        return repository.findAllByPatientIdOrderByMeasuredAtAsc(patientId).stream().map(WeightRecordEntity::toDomain).toList();
    }

    @Override
    public Optional<WeightRecord> findByIdAndPatientId(UUID recordId, UUID patientId) {
        return repository.findByIdAndPatientId(recordId, patientId).map(WeightRecordEntity::toDomain);
    }

    @Override
    public WeightRecord save(WeightRecord record) {
        return repository.save(WeightRecordEntity.from(record)).toDomain();
    }

    @Override
    public void delete(WeightRecord record) {
        repository.deleteById(record.id());
    }
}
