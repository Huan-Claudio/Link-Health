package br.edu.pucgoias.linkhealth.infrastructure.local;

import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoRecord;
import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoRecordRepository;
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
public class InMemoryEvolutionPhotoRecordRepository implements EvolutionPhotoRecordRepository {

    private final ConcurrentMap<UUID, EvolutionPhotoRecord> records = new ConcurrentHashMap<>();

    @Override
    public List<EvolutionPhotoRecord> findByPatientId(UUID patientId) {
        return records.values().stream()
                .filter(record -> record.patientId().equals(patientId))
                .sorted(Comparator.comparing(EvolutionPhotoRecord::capturedAt).reversed())
                .toList();
    }

    @Override
    public Optional<EvolutionPhotoRecord> findByIdAndPatientId(UUID photoId, UUID patientId) {
        return Optional.ofNullable(records.get(photoId))
                .filter(record -> record.patientId().equals(patientId));
    }

    @Override
    public EvolutionPhotoRecord save(EvolutionPhotoRecord record) {
        records.put(record.id(), record);
        return record;
    }

    @Override
    public void delete(EvolutionPhotoRecord record) {
        records.remove(record.id());
    }
}
