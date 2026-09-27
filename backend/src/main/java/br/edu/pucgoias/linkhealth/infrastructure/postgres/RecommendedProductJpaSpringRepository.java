package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RecommendedProductJpaSpringRepository extends JpaRepository<RecommendedProductEntity, UUID> {

    List<RecommendedProductEntity> findAllByPatientIdOrderByCreatedAtAsc(UUID patientId);

    Optional<RecommendedProductEntity> findByIdAndPatientId(UUID id, UUID patientId);
}
