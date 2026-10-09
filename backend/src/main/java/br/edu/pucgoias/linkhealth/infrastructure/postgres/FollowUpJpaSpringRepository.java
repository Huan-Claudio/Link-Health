package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FollowUpJpaSpringRepository extends JpaRepository<FollowUpEntity, UUID> {
    List<FollowUpEntity> findAllByNutritionistIdOrderByInvitedAtAsc(UUID nutritionistId);

    List<FollowUpEntity> findAllByPatientIdOrderByInvitedAtAsc(UUID patientId);
}
