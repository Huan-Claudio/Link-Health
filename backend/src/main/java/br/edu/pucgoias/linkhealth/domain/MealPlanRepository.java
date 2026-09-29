package br.edu.pucgoias.linkhealth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealPlanRepository {
    Optional<MealPlan> findById(UUID id);

    List<MealPlan> findByFollowUpId(UUID followUpId);

    MealPlan save(MealPlan plan);

    void deleteById(UUID id);
}
