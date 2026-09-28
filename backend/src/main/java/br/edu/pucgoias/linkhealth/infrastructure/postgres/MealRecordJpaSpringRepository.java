package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MealRecordJpaSpringRepository extends JpaRepository<MealRecordEntity, UUID> {
    List<MealRecordEntity> findAllByPatientIdOrderByConsumedAtAsc(UUID patientId);
    Optional<MealRecordEntity> findByIdAndPatientId(UUID id, UUID patientId);
}
