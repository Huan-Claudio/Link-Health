package br.edu.pucgoias.linkhealth.infrastructure.local;

import br.edu.pucgoias.linkhealth.domain.MealRecord;
import br.edu.pucgoias.linkhealth.domain.MealRecordRepository;
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
public class InMemoryMealRecordRepository implements MealRecordRepository {

    private final ConcurrentMap<UUID, MealRecord> records = new ConcurrentHashMap<>();

    @Override
    public List<MealRecord> findByPatientId(UUID patientId) {
        return records.values().stream()
                .filter(record -> record.patientId().equals(patientId))
                .sorted(Comparator.comparing(MealRecord::consumedAt))
                .toList();
    }

    @Override
    public Optional<MealRecord> findByIdAndPatientId(UUID recordId, UUID patientId) {
        return Optional.ofNullable(records.get(recordId))
                .filter(record -> record.patientId().equals(patientId));
    }

    @Override
    public MealRecord save(MealRecord record) {
        records.put(record.id(), record);
        return record;
    }

    @Override
    public void delete(MealRecord record) {
        records.remove(record.id());
    }
}
