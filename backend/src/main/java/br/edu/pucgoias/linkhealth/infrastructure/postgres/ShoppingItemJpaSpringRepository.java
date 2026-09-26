package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ShoppingItemJpaSpringRepository extends JpaRepository<ShoppingItemEntity, UUID> {

    List<ShoppingItemEntity> findAllByPatientIdOrderByCreatedAtAsc(UUID patientId);

    Optional<ShoppingItemEntity> findByIdAndPatientId(UUID id, UUID patientId);
}
