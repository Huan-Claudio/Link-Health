package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface WaterIntakeJpaSpringRepository extends JpaRepository<WaterIntakeEntity, UUID> {
    List<WaterIntakeEntity> findAllByPatientIdOrderByConsumedAtAsc(UUID patientId);
    Optional<WaterIntakeEntity> findByIdAndPatientId(UUID id, UUID patientId);
}
