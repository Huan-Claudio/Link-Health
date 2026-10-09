package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.MealPlan;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "meal_plans")
public class MealPlanEntity {
    @Id
    private UUID id;

    @Column(name = "follow_up_id", nullable = false)
    private UUID followUpId;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PlannedMealEntity> meals = new ArrayList<>();

    protected MealPlanEntity() {
    }

    static MealPlanEntity from(MealPlan plan) {
        MealPlanEntity entity = new MealPlanEntity();
        entity.id = plan.id();
        entity.followUpId = plan.followUpId();
        entity.active = plan.active();
        for (int index = 0; index < plan.meals().size(); index++) {
            entity.meals.add(PlannedMealEntity.from(plan.meals().get(index), entity, index));
        }
        return entity;
    }

    void deactivate() {
        active = false;
    }

    MealPlan toDomain() {
        return new MealPlan(id, followUpId, active, meals.stream().map(PlannedMealEntity::toDomain).toList());
    }
}
