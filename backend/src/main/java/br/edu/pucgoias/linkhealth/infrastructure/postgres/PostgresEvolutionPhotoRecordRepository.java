package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoRecord;
import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoRecordRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class PostgresEvolutionPhotoRecordRepository implements EvolutionPhotoRecordRepository {

    private final EvolutionPhotoRecordJpaSpringRepository repository;

    public PostgresEvolutionPhotoRecordRepository(EvolutionPhotoRecordJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<EvolutionPhotoRecord> findByPatientId(UUID patientId) {
        return repository.findAllByPatientIdOrderByCapturedAtDesc(patientId).stream()
                .map(EvolutionPhotoRecordEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<EvolutionPhotoRecord> findByIdAndPatientId(UUID photoId, UUID patientId) {
        return repository.findByIdAndPatientId(photoId, patientId)
                .map(EvolutionPhotoRecordEntity::toDomain);
    }

    @Override
    public EvolutionPhotoRecord save(EvolutionPhotoRecord record) {
        return repository.save(EvolutionPhotoRecordEntity.from(record)).toDomain();
    }

    @Override
    public void delete(EvolutionPhotoRecord record) {
        repository.deleteById(record.id());
    }
}
