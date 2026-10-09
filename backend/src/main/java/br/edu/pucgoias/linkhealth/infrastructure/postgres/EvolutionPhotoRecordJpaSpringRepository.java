package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EvolutionPhotoRecordJpaSpringRepository
        extends JpaRepository<EvolutionPhotoRecordEntity, UUID> {

    List<EvolutionPhotoRecordEntity> findAllByPatientIdOrderByCapturedAtDesc(UUID patientId);

    Optional<EvolutionPhotoRecordEntity> findByIdAndPatientId(UUID id, UUID patientId);
}
