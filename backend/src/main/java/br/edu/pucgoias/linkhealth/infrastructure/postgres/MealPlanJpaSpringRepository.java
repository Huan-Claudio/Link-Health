package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MealPlanJpaSpringRepository extends JpaRepository<MealPlanEntity, UUID> {
    List<MealPlanEntity> findAllByFollowUpId(UUID followUpId);

    List<MealPlanEntity> findAllByFollowUpIdAndActiveTrue(UUID followUpId);
}
