package br.edu.pucgoias.linkhealth.infrastructure.local;

import br.edu.pucgoias.linkhealth.domain.MealPlan;
import br.edu.pucgoias.linkhealth.domain.MealPlanRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryMealPlanRepository implements MealPlanRepository {
    private final ConcurrentMap<UUID, MealPlan> plans = new ConcurrentHashMap<>();

    @Override
    public Optional<MealPlan> findById(UUID id) {
        return Optional.ofNullable(plans.get(id));
    }

    @Override
    public List<MealPlan> findByFollowUpId(UUID followUpId) {
        return plans.values().stream().filter(plan -> plan.followUpId().equals(followUpId)).toList();
    }

    @Override
    public MealPlan save(MealPlan plan) {
        plans.put(plan.id(), plan);
        return plan;
    }

    @Override
    public void deleteById(UUID id) {
        plans.remove(id);
    }
}
