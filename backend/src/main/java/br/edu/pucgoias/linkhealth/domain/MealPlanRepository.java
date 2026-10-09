package br.edu.pucgoias.linkhealth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealPlanRepository {
    Optional<MealPlan> findById(UUID id);

    List<MealPlan> findByFollowUpId(UUID followUpId);

    MealPlan save(MealPlan plan);

    default MealPlan activate(MealPlan plan) {
        findByFollowUpId(plan.followUpId()).stream().filter(MealPlan::active)
                .forEach(current -> save(new MealPlan(current.id(), current.followUpId(), false, current.meals())));
        return save(plan);
    }

    void deleteById(UUID id);
}
