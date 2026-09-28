package br.edu.pucgoias.linkhealth.infrastructure.local;

import br.edu.pucgoias.linkhealth.domain.WeightRecord;
import br.edu.pucgoias.linkhealth.domain.WeightRecordRepository;
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
public class InMemoryWeightRecordRepository implements WeightRecordRepository {

    private final ConcurrentMap<UUID, WeightRecord> records = new ConcurrentHashMap<>();

    @Override
    public List<WeightRecord> findByPatientId(UUID patientId) {
        return records.values().stream()
                .filter(record -> record.patientId().equals(patientId))
                .sorted(Comparator.comparing(WeightRecord::measuredAt))
                .toList();
    }

    @Override
    public Optional<WeightRecord> findByIdAndPatientId(UUID recordId, UUID patientId) {
        return Optional.ofNullable(records.get(recordId))
                .filter(record -> record.patientId().equals(patientId));
    }

    @Override
    public WeightRecord save(WeightRecord record) {
        records.put(record.id(), record);
        return record;
    }

    @Override
    public void delete(WeightRecord record) {
        records.remove(record.id());
    }
}
