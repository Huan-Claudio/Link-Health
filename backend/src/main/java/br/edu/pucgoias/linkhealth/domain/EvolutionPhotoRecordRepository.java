package br.edu.pucgoias.linkhealth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvolutionPhotoRecordRepository {

    List<EvolutionPhotoRecord> findByPatientId(UUID patientId);

    Optional<EvolutionPhotoRecord> findByIdAndPatientId(UUID photoId, UUID patientId);

    EvolutionPhotoRecord save(EvolutionPhotoRecord record);

    void delete(EvolutionPhotoRecord record);
}
