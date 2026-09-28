package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface WeightRecordJpaSpringRepository extends JpaRepository<WeightRecordEntity, UUID> {
    List<WeightRecordEntity> findAllByPatientIdOrderByMeasuredAtAsc(UUID patientId);
    Optional<WeightRecordEntity> findByIdAndPatientId(UUID id, UUID patientId);
}
